package com.org.pdfplatform.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageAndAccentTest {

    @Test
    void exactlyTheRequestedTenLanguagesInTheRequestedGroups() {
        assertEquals(List.of("kn", "ta", "te", "ur", "ml", "hi", "mr", "en", "es", "fr"),
                Arrays.stream(AppLanguage.values()).map(AppLanguage::getCode).toList());
        assertEquals(AppLanguage.Family.INTERNATIONAL, AppLanguage.SPANISH.getFamily());
        assertEquals(AppLanguage.Family.INTERNATIONAL, AppLanguage.FRENCH.getFamily());
        assertEquals("International Languages", AppLanguage.SPANISH.getFamilyLabel());
        assertEquals(AppLanguage.Family.ENGLISH, AppLanguage.ENGLISH.getFamily());
        assertEquals(AppLanguage.Family.OTHER_INDIAN, AppLanguage.HINDI.getFamily());
        assertEquals(AppLanguage.Family.OTHER_INDIAN, AppLanguage.MARATHI.getFamily());
        for (AppLanguage l : new AppLanguage[]{AppLanguage.KANNADA, AppLanguage.TAMIL, AppLanguage.TELUGU, AppLanguage.URDU, AppLanguage.MALAYALAM}) {
            assertTrue(l.isSouthIndian(), l.name());
        }
    }

    @Test
    void everyLanguageHasLocaleNativeNameAndOnlyItsOwnVoiceLocales() {
        for (AppLanguage l : AppLanguage.values()) {
            assertTrue(l.getBcp47().startsWith(l.getCode() + "-"), l.name());
            assertFalse(l.getNativeName().isBlank());
            assertEquals(1, l.getVoiceLocales().size(), l.name() + " has exactly one (standard) voice locale");
            assertEquals(l.getBcp47(), l.getVoiceLocales().get(0));
        }
        assertEquals("ur-IN", AppLanguage.URDU.getBcp47());                    // India, not Pakistan
        assertEquals("en-IN", AppLanguage.ENGLISH.getBcp47());                 // Indian English
    }

    @Test
    void nativeNamesAreInTheirOwnScript() {
        assertEquals("\u0C95\u0CA8\u0CCD\u0CA8\u0CA1", AppLanguage.KANNADA.getNativeName());
        assertEquals("\u0627\u0631\u062F\u0648", AppLanguage.URDU.getNativeName());
    }

    @Test
    void lookupIsStrictForFindAndEnglishForUnknown() {
        assertEquals(AppLanguage.MALAYALAM, AppLanguage.find("ml").orElseThrow());
        assertEquals(AppLanguage.KANNADA, AppLanguage.find("Kannada").orElseThrow());
        assertEquals(AppLanguage.TAMIL, AppLanguage.find("TA-in").orElseThrow());
        assertEquals(AppLanguage.HINDI, AppLanguage.find("hi").orElseThrow());
        assertEquals(AppLanguage.MARATHI, AppLanguage.find("mr-IN").orElseThrow());
        assertTrue(AppLanguage.find(null).isEmpty());
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("xx"));
    }

    @Test
    void everySouthIndianLanguageOffersItsRegionalAccentsAndNothingOverseas() {
        assertTrue(RegionalAccent.forLanguage(AppLanguage.KANNADA).containsAll(List.of(RegionalAccent.MYSURU, RegionalAccent.BENGALURU, RegionalAccent.MANGALURU, RegionalAccent.HUBBALLI)));
        assertTrue(RegionalAccent.forLanguage(AppLanguage.TAMIL).containsAll(List.of(RegionalAccent.CHENNAI, RegionalAccent.MADURAI, RegionalAccent.COIMBATORE)));
        assertTrue(RegionalAccent.forLanguage(AppLanguage.TELUGU).containsAll(List.of(RegionalAccent.VIJAYAWADA, RegionalAccent.HYDERABAD_TELUGU)));
        assertTrue(RegionalAccent.forLanguage(AppLanguage.MALAYALAM).containsAll(List.of(RegionalAccent.KOCHI, RegionalAccent.TRIVANDRUM, RegionalAccent.KOZHIKODE)));
        assertEquals(List.of(RegionalAccent.HYDERABAD_URDU, RegionalAccent.STANDARD), RegionalAccent.forLanguage(AppLanguage.URDU));
        for (AppLanguage l : AppLanguage.values()) {
            assertTrue(RegionalAccent.forLanguage(l).contains(RegionalAccent.STANDARD), l.name());
        }
    }

    @Test
    void spanishFrenchEnglishHindiAndMarathiHaveNoRegionalAccents() {
        for (AppLanguage l : new AppLanguage[]{AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.ENGLISH, AppLanguage.HINDI, AppLanguage.MARATHI}) {
            assertEquals(List.of(RegionalAccent.STANDARD), RegionalAccent.forLanguage(l), l.name());
        }
    }

    @Test
    void noOffshoreAccentExistsAnymore() {
        var names = Arrays.stream(RegionalAccent.values()).map(Enum::name).toList();
        for (String gone : new String[]{"DOHA", "DUBAI", "ABU_DHABI", "MUMBAI", "MUMBAI_MARATHI", "PUNE", "MEXICO", "ARGENTINA", "QUEBEC", "BELGIUM_FRENCH", "SPAIN_CASTILIAN", "FRANCE_PARIS"}) {
            assertFalse(names.contains(gone), gone);
        }
        for (RegionalAccent a : RegionalAccent.values()) {
            assertTrue(List.of("Karnataka", "Tamil Nadu", "Andhra Pradesh", "Telangana", "Kerala", "Deccan", "Standard").contains(a.getCategory()), a.name());
        }
    }

    @Test
    void speechTagIsAlwaysTheLanguagesOwnIndianLocale() {
        assertEquals("ur-IN", RegionalAccent.HYDERABAD_URDU.getBcp47ForLanguage(AppLanguage.URDU));
        assertEquals("kn-IN", RegionalAccent.MYSURU.getBcp47ForLanguage(AppLanguage.KANNADA));
        assertEquals(List.of("ml-IN"), RegionalAccent.KOCHI.voiceLocalesFor(AppLanguage.MALAYALAM));
    }

    @Test
    void unknownAccentCodeIsStandard() {
        assertEquals(RegionalAccent.STANDARD, RegionalAccent.fromCode("doha"));       // a removed accent falls back safely
        assertEquals(RegionalAccent.STANDARD, RegionalAccent.fromCode(null));
        assertEquals(RegionalAccent.KOCHI, RegionalAccent.fromCode("KOCHI"));
    }
}
