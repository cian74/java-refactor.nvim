# Installation

	{
		"cian74/java-refactor.nvim",  -- or whatever your GitHub repo is
		lazy = false,
		dependencies = { "nvim-lua/plenary.nvim", "MunifTanjim/nui.nvim" },
		config = function()
			require("refactor")
		end,
	},

