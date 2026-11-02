package com.ecommerce.user.dto.response;

import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.user.entity.Address;

public record AddressResponse(
		Long id,
		String fullName,
		String phone,
		String district,
		String area,
		String addressLine,
		boolean defaultAddress
) {
	public static AddressResponse from(Address a) {
		return new AddressResponse(a.getId(), a.getFullName(), PhoneNumbers.toLocal(a.getPhone()),
				a.getDistrict(), a.getArea(), a.getAddressLine(), a.isDefaultAddress());
	}
}
