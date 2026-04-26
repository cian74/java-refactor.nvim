# java-refactor.nvim

A Neovim plugin for refactoring Java code.

## Installation

Requires Neovim 0.8+ and Java 11+

## Using Lazy
```lua
{
    "cian74/java-refactor.nvim",
    lazy = false,
    dependencies = { "nvim-lua/plenary.nvim", "MunifTanjim/nui.nvim" },
    config = function()
        require("refactor").setup()
    end,
},
```

## Using packer
```lua
use {
    "cian74/java-refactor.nvim",
    requires = { "nvim-lua/plenary.nvim", "MunifTanjim/nui.nvim" },
    config = function()
        require("refactor").setup()
    end,
}
```

## Building the Backend (if needed)

The plugin comes with a pre-built JAR. If you need to rebuild:

```bash
cd backend
mvn package -DskipTests
cp target/java-refactor-1.0-SNAPSHOT-jar-with-dependencies.jar java-refactor.jar
```

# Configuration

Customize keybindings:

```lua
require("refactor").setup({
    keybindings = {
        menu = "<leader>jf",
        generate_getters_setters = "<leader>gg",
        generate_to_string = "<leader>gt",
        extract_method = "<leader>er",
        extract_variable = "<leader>ev",
        extract_interface = "<leader>ei",
        inline_method = "<leader>im",
        encapsulate_field = "<leader>ef",
        rename = "<leader>rn",
        pull_up = "<leader>pu",
        push_down = "<leader>pd",
        ai_find_usages = "<leader>af",
        settings = "<leader>jr",
    },
})
```

## Default Keybindings

| Action | Default |
|--------|---------|
| Menu | `<leader>jf` |
| Generate Getters/Setters | `<leader>gg` |
| Generate toString | `<leader>gt` |
| Extract Method | `<leader>er` |
| Extract Variable | `<leader>ev` |
| Extract Interface | `<leader>ei` |
| Inline Method | `<leader>im` |
| Encapsulate Field | `<leader>ef` |
| Rename | `<leader>rn` |
| Pull Up | `<leader>pu` |
| Push Down | `<leader>pd` |
| AI Find Usages | `<leader>af` |
| Settings | `<leader>jr` |

## Commands

- `:RefactorMenu` - Open the refactor menu
- `:RefactorHelp` - Show keybindings help
- `:RefactorSettings` - Open settings to change keybindings

## Changing Keybindings at Runtime

Press `<leader>jr` to open the settings menu where you can:
- View all current keybindings
- Change any keybinding by pressing Enter on it
- Keybindings update immediately

## Refactorings

- **Generate Getters/Setters** - Creates getter and setter methods for private fields
- **Generate toString** - Creates a toString() method for the class
- **Extract Method** - Extracts selected code into a new method
- **Extract Variable** - Extracts an expression into a local variable
- **Extract Interface** - Creates an interface from selected methods
- **Inline Method** - Inlines a simple method at its call sites
- **Encapsulate Field** - Converts a field to private with getter/setter
- **Rename** - Renames a method across the codebase
- **Pull Up** - Moves a member to superclass
- **Push Down** - Moves a member to subclass
- **AI Find Usages** - Uses AI to find method references