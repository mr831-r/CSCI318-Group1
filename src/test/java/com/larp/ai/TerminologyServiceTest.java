package com.larp.ai;

import com.larp.editor.InventoryStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminologyServiceTest {

    @Test
    void expandsQueriesWithLearnedTerms() {
        AgentTestSupport s = new AgentTestSupport();
        s.terminology.learn("Blade", "Sword  Dagger", "test");

        assertEquals(List.of("steel", "blade", "sword", "dagger"), List.copyOf(s.terminology.expand("steel BLADE")));
    }

    @Test
    void multiWordTermsMatchInsideTheQuery() {
        AgentTestSupport s = new AgentTestSupport();
        s.terminology.learn("pointy stick", "spear lance", "test");

        assertTrue(s.terminology.expand("any pointy stick left?").contains("lance"));
    }

    @Test
    void searchToolUsesLearnedTerms() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1019", "Cavalry lance", 9, InventoryStatus.AVAILABLE);
        assertTrue(s.tools.searchInventory("pointy stick").startsWith("No inventory items matched"));

        s.terminology.learn("pointy stick", "spear lance", "test");

        assertTrue(s.tools.searchInventory("pointy stick").contains("ITEM-1019"));
    }

    @Test
    void rejectsEmptyTerms() {
        AgentTestSupport s = new AgentTestSupport();
        assertThrows(IllegalArgumentException.class, () -> s.terminology.learn(" ", "sword", "test"));
    }
}
