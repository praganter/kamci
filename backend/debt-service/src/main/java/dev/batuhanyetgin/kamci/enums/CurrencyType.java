package dev.batuhanyetgin.kamci.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurrencyType {
    USD("$"),
    EUR("€"),
    TRY("₺"),
    GBP("£"),
    JPY("¥"),
    CNY("¥"),
    CAD("CA$"),
    AUD("A$"),
    CHF("Fr"),
    RUB("₽"),
    INR("₹"),
    BRL("R$"),
    KRW("₩"),
    SAR("﷼"),
    AED("د.إ");

    private final String symbol;
}
