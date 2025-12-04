package com.example.logicmind.additional

import java.util.Locale

object WordBank {
    private val words = mapOf(
        "pl" to mapOf(
            3 to listOf(
                "KOT", "LAS", "DOM", "RAK", "MUS", "SOK", "LIS", "TOR", "NOC", "CEL",
                "LEK", "KOS", "KOC", "BAT", "BAL", "GRA", "TIR", "MUR", "OKO", "HAK",
                "ROK", "GOL", "LOS", "MOC", "ZOO", "ELF", "LEW", "ŻAL", "ĆMA", "PAS",
                "BÓG", "DĄB", "GĘŚ", "JEŻ", "WĄŻ", "SĘP", "ŻUK", "SÓL", "PAN", "ŁAD"
            ),
            4 to listOf(
                "MAMA", "TATA", "KAWA", "RYBA", "AUTO", "PIES", "KINO", "RANA", "FALA", "MAPA",
                "KREM", "STÓŁ", "KRÓL", "OKNO", "OWOC", "PIŁA", "LINA", "WODA", "SOWA", "KURA",
                "WINO", "MOST", "BRAT", "ŁÓDŹ", "RURA", "BAŚŃ", "LIST", "KURS", "KASA", "BANK",
                "ŻABA", "DUCH", "MĄKA", "RĘKA", "NOGA", "KOŚĆ", "WĄSY", "ZĘBY", "GŁAZ", "PŁOT"
            ),
            5 to listOf(
                "BANAN", "DROGA", "AFERA", "SPORT", "DOMEK", "PRACA", "PIŁKA", "WIARA", "KREDA", "DESKA",
                "KLUCZ", "ZAMEK", "PLAŻA", "MASŁO", "MLEKO", "SZOSA", "PLAMA", "KRZYK", "KWIAT", "LISTA",
                "RAMPA", "KARTA", "WALKA", "STOPA", "GŁOWA", "ZŁOTO", "SERCE", "OBRAZ", "TRAWA", "RZEKA",
                "TĘCZA", "BAGAŻ", "WIEŚĆ", "PÓŁKA", "ŁYŻKA", "JĘZYK", "CHATA", "ŚLEDŹ", "GĄBKA", "ŻEBRO"
            ),
            6 to listOf(
                "PACZKA", "SZKOŁA", "JABŁKO", "SERNIK", "KOTLET", "TALERZ", "WALIZA", "KOCIOŁ", "LEKARZ", "KAKTUS",
                "DRZEWO", "WIADRO", "BIURKO", "POLANA", "POZIOM", "LUSTRO", "KRAWAT", "KRÓLIK", "BOCIAN", "KAPCIE",
                "ANANAS", "MONETA", "PAPIER", "KREDYT", "TARCZA", "CIASTO", "CHMURA", "KLUCZE", "WĘGIEL", "STOLIK",
                "ŻOŁĄDŹ", "MUZYKA", "KAMIEŃ", "WIOSNA", "ŹRÓDŁO", "KASZEL", "ZESZYT", "OŁÓWEK", "SUKNIA", "KORALE"
            ),
            7 to listOf(
                "PLANETA", "ŻARÓWKA", "TELEFON", "LATARKA", "PORTFEL", "PREZENT", "KANAPKA", "STRAŻAK", "POSIŁEK", "SPODNIE",
                "GWIAZDA", "PARASOL", "KOLEJKA", "ZABAWKA", "PROBLEM", "PIEROGI", "MASZYNA", "STUDENT", "KAWALER", "KOPERTA",
                "KAPUSTA", "OKULARY", "POMIDOR", "SAMOLOT", "ROWEREK", "PARAWAN", "FOTELIK", "POLICJA", "GOSPODA", "KRZESŁO",
                "AGRAFKA", "ŁOPATKA", "CYTRYNA", "SEKUNDA", "KOKARDA", "ZEGAREK", "WALIZKA", "KOCIĘTA", "KOSZMAR", "TOREBKA"
            ),
            8 to listOf(
                "KOMPUTER", "ZWIERZAK", "PIOSENKA", "ZMYWARKA", "DOKUMENT", "CZŁOWIEK", "ASTRONOM", "ZAWODNIK", "UCZELNIA", "ZIEMNIAK",
                "SAMOCHÓD", "MUZYKANT", "KLAWISZE", "PRZYCISK", "LOTNISKO", "POLITYKA", "SATELITA", "DŁUGOPIS", "POKRZYWA", "PRZYSTAŃ",
                "OPOWIEŚĆ", "PRZYRODA", "POZIOMKA", "NOTATNIK", "KIEROWCA", "OGRODNIK", "EKONOMIA", "PRZYGODA", "JEDZENIE", "DOMOWNIK",
                "DYREKTOR", "MARZENIE", "JASKÓŁKA", "STRZYŻYK", "ŻURAWINA", "PAMIĄTKA", "FOTOGRAF", "ZDOLNOŚĆ", "URZĘDNIK", "ROZDANIE"
            ),
            9 to listOf(
                "ARCHITEKT", "SPOTKANIE", "BALETNICA", "STOKROTKA", "NIEBIESKI", "KONDUKTOR", "CZEKOLADA", "UKŁADANKA", "PODARUNEK", "TRUSKAWKA",
                "MOTYWACJA", "LISTONOSZ", "POLICJANT", "SKARPETKI", "WINOGRONO", "BRUKSELKA", "PARASOLKA", "BIEDRONKA", "TELEWIZJA", "KONTROLER",
                "LITERACKI", "ORKIESTRA", "TAJEMNICA", "TORNISTER", "SŁUCHAWKI", "PODKŁADKA", "KAMIZELKA", "PROSTOKĄT", "PENSJONAT", "CUKROWNIA",
                "MALARSTWO", "GEOGRAFIA", "MRÓWKOJAD", "HULAJNOGA", "CHRZĄSZCZ", "ŚWIERSZCZ", "PIEKARNIA", "PORCELANA", "MIRABELKA", "LICYTACJA"
            ),
            10 to listOf(
                "KLAWIATURA", "KOSMONAUTA", "HELIKOPTER", "BIBLIOTEKA", "PRZYJACIEL", "PIETRUSZKA", "LOKOMOTYWA", "UTLENIANIE", "OGŁOSZENIE", "MATEMATYKA",
                "ASTRONAUTA", "KIEROWNICA", "SCENARIUSZ", "NAUCZYCIEL", "ARCYKSIĄŻĘ", "SPRZEDAWCA", "KOSZYKÓWKA", "BRATERSTWO", "ROZRZUTNIK", "AKUMULATOR",
                "INFORMATYK", "ZWIEDZANIE", "ODŻYWIANIE", "DOSTĘPNOŚĆ", "WENTYLATOR", "SZERMIERKA", "SZKICOWNIK", "CZASOPISMO", "METEOROLOG", "BALUSTRADA",
                "ROWERZYSTA", "SPADOCHRON", "NIEDŹWIEDŹ", "GOŚCINNOŚĆ", "ZAGROŻENIE", "KSIĘGOWOŚĆ", "FARMACEUTA", "DESPERACJA", "SZCZĘŚLIWY", "PRZYSZŁOŚĆ"
            )
        ),
        "en" to mapOf(
            3 to listOf(
                "CAT", "SUN", "DOG", "RUN", "BED", "SKY", "FLY", "PIG", "RAT", "ANT",
                "BAT", "MAP", "CAP", "TAP", "RED", "INK", "PEN", "CUP", "MUG", "BUS",
                "CAR", "VAN", "KEY", "BOX", "FOX", "COW", "HEN", "EGG", "ARM", "LEG",
                "ICE", "HOT", "OLD", "NEW", "BIG", "DAY", "WAR", "OIL", "TEA", "EAT"
            ),
            4 to listOf(
                "FOUR", "TREE", "ROAD", "FISH", "GAME", "CODE", "MOOD", "BOOK", "MOON", "FIRE",
                "DESK", "LAMP", "DOOR", "WALL", "ROOF", "FROG", "DUCK", "GOAT", "LION", "BEAR",
                "WOLF", "BANK", "CASH", "COIN", "CARD", "TIME", "DATE", "YEAR", "WEEK", "HAND",
                "FACE", "FEET", "HEAD", "HAIR", "LOVE", "HATE", "FOOD", "GOLD", "SHIP", "STAR"
            ),
            5 to listOf(
                "APPLE", "WATER", "BOARD", "MOUSE", "PHONE", "TIGER", "EARTH", "TRAIN", "HOUSE", "LIGHT",
                "CLOCK", "WATCH", "TABLE", "CHAIR", "WHITE", "BLACK", "GREEN", "BREAD", "RIVER", "SPOON",
                "PIZZA", "KNIFE", "PLATE", "RULER", "SUGAR", "HEART", "BRAIN", "JUICE", "STONE", "BRICK",
                "MONEY", "MUSIC", "VOICE", "SOUND", "PLANE", "BEACH", "FRUIT", "GRAPE", "WORLD", "PRICE"
            ),
            6 to listOf(
                "ORANGE", "BANANA", "PLANET", "GUITAR", "MARKET", "CHEESE", "DRIVER", "CAMERA", "PENCIL", "ERASER",
                "PUZZLE", "WINDOW", "MIRROR", "GARDEN", "BOTTLE", "JACKET", "TICKET", "CARROT", "TENNIS", "PEPPER",
                "HAMMER", "LADDER", "DOCTOR", "CHERRY", "GARAGE", "POLICE", "POTATO", "TURTLE", "TOMATO", "SPIDER",
                "BUTTER", "CIRCLE", "SQUARE", "NUMBER", "LETTER", "FLOWER", "CLOUDS", "SUMMER", "WINTER", "FOREST"
            ),
            7 to listOf(
                "JOURNEY", "COUNTRY", "BICYCLE", "AIRPORT", "MORNING", "EVENING", "WEATHER", "LIBRARY", "SPEAKER", "EXAMPLE",
                "KITCHEN", "BEDROOM", "MONITOR", "DIGITAL", "SUPPORT", "CHICKEN", "BLANKET", "MONSTER", "KINGDOM", "VICTORY",
                "LETTUCE", "CABBAGE", "PROBLEM", "FREEDOM", "PICTURE", "BALANCE", "FACTORY", "HOLIDAY", "OCTOPUS", "DOLPHIN",
                "MESSAGE", "CHAPTER", "REQUEST", "SERVICE", "QUALITY", "PROJECT", "NETWORK", "SCIENCE", "WELCOME", "ADDRESS"
            ),
            8 to listOf(
                "ELEPHANT", "LANGUAGE", "UMBRELLA", "PLATFORM", "HOSPITAL", "INTERNET", "STRATEGY", "DOCUMENT", "JUDGMENT", "CALENDAR",
                "KEYBOARD", "BATHROOM", "PARADISE", "TOGETHER", "OPERATOR", "FAVORITE", "EDUCATOR", "FOOTBALL", "ABSOLUTE", "BASEBALL",
                "COMPUTER", "SWIMMING", "MEDICINE", "NOTEBOOK", "QUESTION", "CUCUMBER", "AUDIENCE", "DAUGHTER", "BUILDING", "IDENTITY",
                "ACTIVITY", "MATERIAL", "MOVEMENT", "INDUSTRY", "EXERCISE", "DISTANCE", "LOCATION", "SECURITY", "POSITION", "THOUSAND"
            ),
            9 to listOf(
                "DISCOVERY", "EXCELLENT", "LANDSCAPE", "ADVENTURE", "OPERATIVE", "DIRECTION", "KNOWLEDGE", "VEGETABLE", "VOLUNTEER", "INFLUENCE",
                "RASPBERRY", "BLUEBERRY", "DANGEROUS", "PINEAPPLE", "IMPORTANT", "WORKPLACE", "TELEPHONE", "CHOCOLATE", "HAPPINESS", "WONDERFUL",
                "ASPARAGUS", "ARTICHOKE", "NEWSPAPER", "BEAUTIFUL", "EDUCATION", "ALLIGATOR", "CROCODILE", "PRESIDENT", "ALGORITHM", "MACHINERY",
                "BREAKFAST", "DIFFERENT", "CHEMISTRY", "COMMITTEE", "CHARACTER", "FREQUENCY", "INVISIBLE", "NUTRITION", "COMPLAINT", "AUTHORITY"
            ),
            10 to listOf(
                "MOTORCYCLE", "GOVERNMENT", "DICTIONARY", "RESTAURANT", "TECHNOLOGY", "ATTRACTION", "GRAPEFRUIT", "EFFICIENCY", "BACKGROUND", "DISHWASHER",
                "BRAINSTORM", "STRAWBERRY", "MICROPHONE", "VOLLEYBALL", "LEMONGRASS", "CHICKENPOX", "TOOTHBRUSH", "TOOTHPASTE", "JOURNALIST", "TELEVISION",
                "LITERATURE", "CONFIDENCE", "FRIENDSHIP", "PARLIAMENT", "DOORHANDLE", "VULNERABLE", "BASKETBALL", "BENEFICIAL", "POPULATION", "MOTIVATION",
                "EXPERIENCE", "CONTINUOUS", "DIFFICULTY", "DEPARTMENT", "PARTICULAR", "PROFESSION", "REASONABLE", "STATISTICS", "SUCCESSFUL", "CONSISTENT"
            )
        )
    )

    fun getWords(lang: String, minLength: Int, maxLength: Int, count: Int): List<String> {
        val langKey = lang.lowercase(Locale.ROOT)

        val availableWords = words[langKey]?.flatMap { (length, wordList) ->
            if (length in minLength..maxLength) {
                wordList
            } else {
                emptyList()
            }
        } ?: emptyList()

        if (availableWords.isEmpty()) {
            return emptyList()
        }

        return availableWords.shuffled().take(count)
    }

    fun getWords(lang: String, maxLength: Int, count: Int): List<String> {
        return getWords(lang, 3, maxLength, count)
    }
}