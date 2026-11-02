package com.ecommerce.notification.service;

import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.model.PageResponse;
import com.ecommerce.common.web.Pages;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.entity.OutboxEmail;
import com.ecommerce.notification.repository.OutboxEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationAdminService {

	private final OutboxEmailRepository outboxRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public PageResponse<NotificationResponse> search(OutboxEmail.Status status, String orderNumber, int pageNo, int pageSize) {
		String q = orderNumber == null ? "" : orderNumber.trim().toUpperCase(Locale.ROOT);
		return PageResponse.of(outboxRepository.search(status, q,
				Pages.of(pageNo, pageSize, AppConstants.SORT_BY_VALUE, AscOrDescType.desc, Set.of(AppConstants.SORT_BY_VALUE))),
				NotificationResponse::from);
	}

	/** Puts a FAILED email back in the queue (e.g. after fixing SMTP credentials). */
	@Transactional
	public NotificationResponse retry(Long id) {
		OutboxEmail e = outboxRepository.findById(id).orElseThrow(() -> ResponseException.notFound(ErrorCode.NOTIFICATION_NOT_FOUND));
		if (e.getStatus() != OutboxEmail.Status.FAILED) {
			throw ResponseException.conflict(ErrorCode.NOTIFICATION_NOT_RETRYABLE);
		}
		e.setStatus(OutboxEmail.Status.PENDING);
		e.setAttempts(0);
		e.setNextAttemptAt(clock.instant());
		e.setLastError(null);
		return NotificationResponse.from(e);
	}
}
