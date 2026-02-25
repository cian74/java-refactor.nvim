package com.cian.refactor.strategy;

import com.cian.refactor.Refactored;

public interface RefactoringStrategy {
    String getCommandName();
    Refactored apply(String source);
    Refactored apply(String source, Object... params);
}
