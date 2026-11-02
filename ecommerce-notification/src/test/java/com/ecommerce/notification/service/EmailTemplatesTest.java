package com.ecommerce.notification.service;

import com.ecommerce.common.event.OrderNotificationEvent.Type;
import com.ecommerce.notification.Fixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

	private final EmailTemplates templates = new EmailTemplates(Fixtures.props("log", null));

	@Test
	void shopperAndCatalogTextIsEscapedInHtml() {
		var r = templates.render(Fixtures.event(Type.PLACED, "a@b.test", "COD", false, "<script>alert(1)</script> Rafi"));
		assertThat(r.html()).doesNotContain("<script>").contains("&lt;script&gt;").contains("Kitsune mask &lt;cosplay&gt; set");
		assertThat(r.text()).contains("Kitsune mask <cosplay> set (Size: M)");
	}

	@Test
	void placedTellsCodShoppersWhatToHaveReadyAndOnlineShoppersToWait() {
		var cod = templates.render(Fixtures.event(Type.PLACED, "a@b.test", "COD", false, "Rafi Islam"));
		assertThat(cod.subject()).isEqualTo("Order SS-7K2M9QXA received - Senpai Supply");
		assertThat(cod.text()).contains("Hi Rafi,").contains("have ৳ 8,400 ready").contains("Delivery: Free")
				.contains("https://shop.test/track?order=SS-7K2M9QXA");

		var bkash = templates.render(Fixtures.event(Type.PLACED, "a@b.test", "BKASH", true, "Rafi"));
		assertThat(bkash.text()).contains("bKash payment is confirmed").doesNotContain("ready");
	}

	@Test
	void eachStatusHasItsOwnSubjectAndLink() {
		assertThat(templates.render(Fixtures.event(Type.SHIPPED, "a@b.test", "COD", false, "R")).subject()).contains("on its way");
		var delivered = templates.render(Fixtures.event(Type.DELIVERED, "a@b.test", "COD", false, "R"));
		assertThat(delivered.text()).contains("https://shop.test/orders/SS-7K2M9QXA");
		assertThat(templates.render(Fixtures.event(Type.CANCELLED, "a@b.test", "CARD", true, "R")).text()).contains("money goes back");
		assertThat(templates.render(Fixtures.event(Type.PAYMENT_RECEIVED, "a@b.test", "CARD", true, "R")).subject()).startsWith("Payment received");
	}

	@Test
	void moneyKeepsPaisaOnlyWhenThereAreAny() {
		assertThat(EmailTemplates.money(new BigDecimal("1420.00"))).isEqualTo("৳ 1,420");
		assertThat(EmailTemplates.money(new BigDecimal("1420.50"))).isEqualTo("৳ 1,420.50");
	}
}
