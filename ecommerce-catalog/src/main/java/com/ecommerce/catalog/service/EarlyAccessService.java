package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.EarlyAccessRequest;
import com.ecommerce.catalog.dto.response.EarlyAccessSignupResponse;
import com.ecommerce.catalog.entity.Category;
import com.ecommerce.catalog.entity.EarlyAccessSignup;
import com.ecommerce.catalog.repository.CategoryRepository;
import com.ecommerce.catalog.repository.EarlyAccessSignupRepository;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.PageResponse;
import com.ecommerce.common.phone.PhoneNumbers;
import com.ecommerce.common.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EarlyAccessService {

	private final CategoryRepository categoryRepository;
	private final EarlyAccessSignupRepository signupRepository;
	private final RateLimiter rateLimiter;

	/**
	 * Adds the contact to the list once per category. Signing up again answers the same, so the
	 * endpoint never reveals whether a number or email is already on a list.
	 */
	@Transactional
	public void signUp(EarlyAccessRequest request, String ip) {
		rateLimiter.hit("early-access:ip:" + ip, 10, Duration.ofHours(1));
		Category category = categoryRepository.findBySlug(request.getCategorySlug().trim())
				.filter(c -> c.isActive() && c.isComingSoon())
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.EARLY_ACCESS_CLOSED));
		String phone = blank(request.getPhone()) ? null
				: PhoneNumbers.normalize(request.getPhone()).orElseThrow(() -> ResponseException.badRequest(ErrorCode.INVALID_PHONE));
		String email = blank(request.getEmail()) ? null : request.getEmail().trim().toLowerCase(Locale.ROOT);
		if (phone == null && email == null) {
			throw ResponseException.badRequest(ErrorCode.EARLY_ACCESS_CONTACT_REQUIRED);
		}
		boolean known = (phone != null && signupRepository.existsByCategoryIdAndPhone(category.getId(), phone))
				|| (email != null && signupRepository.existsByCategoryIdAndEmailIgnoreCase(category.getId(), email));
		if (known) {
			return;
		}
		EarlyAccessSignup signup = new EarlyAccessSignup();
		signup.setCategory(category);
		signup.setPhone(phone);
		signup.setEmail(email);
		signupRepository.save(signup);
	}

	@Transactional(readOnly = true)
	public PageResponse<EarlyAccessSignupResponse> list(String categorySlug, int pageNo, int pageSize) {
		PageRequest page = PageRequest.of(Math.max(0, pageNo), Math.min(Math.max(1, pageSize), 100), Sort.by(Sort.Direction.DESC, "creationDate"));
		if (blank(categorySlug)) {
			return PageResponse.of(signupRepository.findAllBy(page), EarlyAccessSignupResponse::from);
		}
		Category category = categoryRepository.findBySlug(categorySlug.trim())
				.orElseThrow(() -> ResponseException.notFound(ErrorCode.CATEGORY_NOT_FOUND));
		return PageResponse.of(signupRepository.findByCategoryId(category.getId(), page), EarlyAccessSignupResponse::from);
	}

	private static boolean blank(String s) {
		return s == null || s.isBlank();
	}
}
