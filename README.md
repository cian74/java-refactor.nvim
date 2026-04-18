# Installation

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
        inline_method = "<leader>im",
        flame_graph = "<leader>pf",
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
| Inline Method | `<leader>im` |
| Flame Graph | `<leader>pf` |
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
- **Inline Method** - Inlines a simple method at its call sites
- **Flame Graph** - Profiles the current Java method
