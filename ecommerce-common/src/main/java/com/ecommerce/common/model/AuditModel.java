package com.ecommerce.common.model;

import com.ecommerce.common.constant.AuditColumns;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Every entity extends this. Timestamps are {@link Instant}, mapped to PostgreSQL
 * {@code timestamptz}: every time in the system is UTC. Ids are BIGSERIAL; anything that
 * appears in a public URL uses a separate slug or number column instead of the id.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = AuditColumns.ID)
	private Long id;

	@CreatedBy
	@Column(name = AuditColumns.CREATED_BY, updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column(name = AuditColumns.LAST_MODIFIED_BY)
	private String lastModifiedBy;

	@CreatedDate
	@Column(name = AuditColumns.CREATION_DATE, updatable = false)
	private Instant creationDate;

	@LastModifiedDate
	@Column(name = AuditColumns.LAST_MODIFIED_DATE)
	private Instant lastModifiedDate;
}
