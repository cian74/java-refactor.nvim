package com.cian.refactor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefactoringEngineCapitaliseTest {

    @Test
    void testCapitaliseNormal() {
        RefactoringEngine engine = new RefactoringEngine();
        assertEquals("Name", engine.capitalise("name"));
    }

    @Test
    void testCapitaliseEmpty() {
        RefactoringEngine engine = new RefactoringEngine();
        assertEquals("", engine.capitalise(""));
    }

    @Test
    void testCapitaliseNull() {
        RefactoringEngine engine = new RefactoringEngine();
        assertNull(engine.capitalise(null));
    }
}

