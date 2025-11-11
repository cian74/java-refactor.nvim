# Installation

Requires Neovim 0.8+ 

## Using Lazy
```lua
	{
		"cian74/java-refactor.nvim",  
		lazy = false,
		dependencies = { "nvim-lua/plenary.nvim", "MunifTanjim/nui.nvim" },
		config = function()
			require("refactor")
		end,
	},
```
## Using packer
```lua
	use {
	  "cian74/java-refactor.nvim",
	  requires = { "nvim-lua/plenary.nvim", "MunifTanjim/nui.nvim" },
	  config = function()
	    require("refactor")
	  end,
	}
```

