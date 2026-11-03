package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.request.CharacterRequest;
import com.ecommerce.catalog.dto.response.CharacterResponse;
import com.ecommerce.catalog.entity.ShopCharacter;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.catalog.repository.ShopCharacterRepository;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CharacterService {

	private final ShopCharacterRepository characterRepository;
	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public List<CharacterResponse> listActive() {
		Map<Long, Long> counts = activeCounts();
		return characterRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
				.map(c -> CharacterResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList();
	}

	@Transactional(readOnly = true)
	public List<CharacterResponse> listAll() {
		Map<Long, Long> counts = activeCounts();
		return characterRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(c -> CharacterResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList();
	}

	@Transactional
	public CharacterResponse create(CharacterRequest request) {
		if (characterRepository.existsBySlug(request.getSlug())) {
			throw ResponseException.conflict(ErrorCode.CHARACTER_SLUG_TAKEN);
		}
		ShopCharacter character = new ShopCharacter();
		apply(character, request);
		return CharacterResponse.from(characterRepository.save(character), 0);
	}

	@Transactional
	public CharacterResponse update(Long id, CharacterRequest request) {
		ShopCharacter character = find(id);
		if (characterRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
			throw ResponseException.conflict(ErrorCode.CHARACTER_SLUG_TAKEN);
		}
		apply(character, request);
		return CharacterResponse.from(character, activeCounts().getOrDefault(id, 0L));
	}

	@Transactional
	public void delete(Long id) {
		ShopCharacter character = find(id);
		if (productRepository.existsByCharacterId(id)) {
			throw ResponseException.conflict(ErrorCode.CHARACTER_NOT_EMPTY);
		}
		characterRepository.delete(character);
	}

	ShopCharacter find(Long id) {
		return characterRepository.findById(id).orElseThrow(() -> ResponseException.notFound(ErrorCode.CHARACTER_NOT_FOUND));
	}

	private Map<Long, Long> activeCounts() {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : productRepository.countByCharacter(ProductStatus.ACTIVE)) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return counts;
	}

	private void apply(ShopCharacter c, CharacterRequest r) {
		c.setSlug(r.getSlug());
		c.setName(r.getName().trim());
		c.setLabel(blankToNull(r.getLabel()));
		c.setBlurb(blankToNull(r.getBlurb()));
		c.setSortOrder(r.getSortOrder());
		c.setActive(r.isActive());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
