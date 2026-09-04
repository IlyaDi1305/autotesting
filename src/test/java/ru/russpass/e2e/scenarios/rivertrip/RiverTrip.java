package ru.russpass.e2e.scenarios.rivertrip;

public enum RiverTrip {

    CRIMEAN_BRIDGE_TO_RUSSIA(
            "6a904924ebff13cc47856bf9",
            "Прогулка по маршруту «Крымский мост» — «Национальный центр \"Россия\"»"
    ),

    RUSSIA_TO_CRIMEAN_BRIDGE(
            "6a904923ebff13cc47856ba3",
            "Прогулка по маршруту «Национальный центр \"Россия\"» — «Крымский мост»"
    ),

    KLENOVY_TO_KITAY_GOROD(
            "6a904923ebff13cc47856b81",
            "Прогулка по маршруту «Кленовый бульвар» — «Китай-город»"
    ),

    PARK_GORKOGO(
            "6a903554ebff13cc47846c27",
            "Прогулка по маршруту «Парк Горького-2» — «Парк Горького-2»"
    ),

    KLENOVY_TO_KIEVSKY_1(
            "6a903137d23c29d27f328cc4",
            "Прогулка по маршруту «Кленовый бульвар» — «Киевский»"
    ),

    SEVERNY_EXPRESS(
            "6a8daeb4a168f2c35640a93d",
            "Северный-экспресс"
    ),

    SEVERNY(
            "6a8daeb40e2fabae30c83976",
            "Северный"
    ),

    SRV_ZAKHARKOVO(
            "6a8daeb40e2fabae30c83958",
            "СРВ - Захарково"
    ),

    SRV_KHIMKI(
            "6a8daeb3a168f2c35640a910",
            "СРВ - Химки"
    ),

    HISTORICAL(
            "6a8daeb3a168f2c35640a8ec",
            "Исторический"
    ),

    KOLOMENSKY_EXPRESS(
            "6a8daeb30e2fabae30c8393a",
            "Коломенский-экспресс"
    ),

    CRIMEAN_BRIDGE_ROUND(
            "6a8daeaf0e2fabae30c838f5",
            "Прогулка по маршруту «Крымский мост» — «Крымский мост»"
    ),

    RUSSIA_ROUND(
            "6a8daeaf0e2fabae30c838d4",
            "Прогулка по маршруту «Национальный центр \"Россия\"» — «Национальный центр \"Россия\"»"
    ),

    TRETYAKOVSKY_ROUND(
            "6a8daeaf0e2fabae30c838b6",
            "Прогулка по маршруту «Третьяковский» — «Третьяковский»"
    ),

    KLENOVY_TO_KIEVSKY_2(
            "6a8daeaea168f2c35640a8b9",
            "Прогулка по маршруту «Кленовый бульвар» — «Киевский»"
    ),

    SEVERNY_RIVER_STATION_ROUND(
            "6a8daeaea168f2c35640a89b",
            "Прогулка по маршруту «Северный речной вокзал» — «Северный речной вокзал»"
    ),

    ZARYADYE_ROUND(
            "6a8daeae0e2fabae30c83886",
            "Прогулка по маршруту «Зарядье» — «Зарядье»"
    ),

    RADISSON_ROYAL(
            "69cb692c7a9e725b288de57c",
            "Речка - Речная прогулка от Флотилии «Рэдиссон Ройал», причал «Гостиница Украина»"
    );

    private final String id;
    private final String name;

    RiverTrip(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }
}