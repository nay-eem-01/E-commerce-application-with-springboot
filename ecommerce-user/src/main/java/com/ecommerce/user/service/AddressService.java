package com.ecommerce.user.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressResponse;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Every lookup is scoped to the caller: someone else's address id is a 404. */
@Service
@RequiredArgsConstructor
public class AddressService {

	static final int MAX_ADDRESSES = 10;

	private final AddressRepository addressRepository;

	@Transactional(readOnly = true)
	public List<AddressResponse> list(Long userId) {
		return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
				.map(AddressResponse::from).toList();
	}

	@Transactional
	public AddressResponse create(Long userId, AddressRequest request) {
		long count = addressRepository.countByUserId(userId);
		if (count >= MAX_ADDRESSES) {
			throw ResponseException.badRequest(ErrorCode.ADDRESS_LIMIT);
		}
		Address address = new Address();
		address.setUserId(userId);
		apply(address, request);
		address.setDefaultAddress(request.isDefaultAddress() || count == 0);
		Address saved = addressRepository.save(address);
		if (saved.isDefaultAddress()) {
			addressRepository.clearDefaultExcept(userId, saved.getId());
		}
		return AddressResponse.from(saved);
	}

	@Transactional
	public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
		Address address = find(userId, addressId);
		apply(address, request);
		if (request.isDefaultAddress()) {
			address.setDefaultAddress(true);
			addressRepository.clearDefaultExcept(userId, addressId);
		}
		return AddressResponse.from(address);
	}

	@Transactional
	public void delete(Long userId, Long addressId) {
		addressRepository.delete(find(userId, addressId));
	}

	/** Saves the checkout address when the shopper has none yet (design: "saved the first time you check out"). */
	@Transactional
	public void saveFirstAddress(Long userId, String fullName, String phone, String district, String area, String line) {
		if (addressRepository.countByUserId(userId) > 0) {
			return;
		}
		Address address = new Address();
		address.setUserId(userId);
		address.setFullName(fullName);
		address.setPhone(phone);
		address.setDistrict(district);
		address.setArea(area);
		address.setAddressLine(line);
		address.setDefaultAddress(true);
		addressRepository.save(address);
	}

	private Address find(Long userId, Long addressId) {
		return addressRepository.findByIdAndUserId(addressId, userId)
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.ADDRESS_NOT_FOUND));
	}

	private void apply(Address address, AddressRequest request) {
		address.setFullName(request.getFullName().trim());
		address.setPhone(PhoneNumbers.normalize(request.getPhone())
				.orElseThrow(() -> ResponseException.badRequest(ErrorCode.INVALID_PHONE)));
		address.setDistrict(request.getDistrict().trim());
		address.setArea(request.getArea().trim());
		address.setAddressLine(request.getAddressLine().trim());
	}
}
