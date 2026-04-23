local Menu = require("nui.menu")
local config = require("refactor.config")

local M = {}

local settings_menu = nil

local actions_list = {
	{ key = "menu", label = "Open Refactor Menu" },
	{ key = "generate_getters_setters", label = "Generate Getters/Setters" },
	{ key = "generate_to_string", label = "Generate toString" },
	{ key = "extract_method", label = "Extract Method" },
	{ key = "extract_variable", label = "Extract Variable" },
	{ key = "extract_interface", label = "Extract Interface" },
	{ key = "ai_find_usages", label = "AI Find Usages" },
	{ key = "inline_method", label = "Inline Method" },
	{ key = "encapsulate_field", label = "Encapsulate Field" },
	{ key = "rename", label = "Rename" },
	{ key = "pull_up", label = "Pull Up" },
	{ key = "push_down", label = "Push Down" },
	{ key = "flame_graph", label = "Flame Graph" },
	{ key = "settings", label = "Open Settings" },
}

local function format_keybinding(key)
	if not key then return "Not set" end
	return key
end

function M.show_settings()
	if settings_menu then
		pcall(function()
			if settings_menu:is_mounted() then
				settings_menu:unmount()
			end
		end)
	end

	local function make_lines()
		local lines = {}
		for i, action in ipairs(actions_list) do
			local key = config.get_keybinding(action.key)
			table.insert(lines, Menu.item(("%d. %s"):format(i, action.label)))
		end
		return lines
	end

	settings_menu = Menu({
		position = "50%",
		size = {
			width = 50,
			height = 17,
		},
		border = {
			style = "rounded",
			text = {
				top = "[ Keybinding Settings ]",
				top_align = "center",
			},
		},
		win_options = {
			winhighlight = "Normal:Normal,FloatBorder:Normal",
		},
	}, {
		lines = make_lines(),
		keymap = {
			focus_next = { "j", "<Down>", "<Tab>" },
			focus_prev = { "k", "<Up>", "<S-Tab>" },
			close = { "<Esc>", "q", "<C-c>" },
			submit = { "<CR>" },
		},
		on_close = function()
			settings_menu = nil
		end,
		on_submit = function(item)
			local idx = tonumber(item.text:match("^%d+"))
			if idx and actions_list[idx] then
				M.prompt_for_keybinding(actions_list[idx].key, actions_list[idx].label)
			end
		end,
	})

	settings_menu:mount()
end

function M.prompt_for_keybinding(action_key, action_label)
	if settings_menu then
		settings_menu:unmount()
		settings_menu = nil
	end

	local prompt = ("New keybinding for %s (current: %s): "):format(
		action_label,
		format_keybinding(config.get_keybinding(action_key))
	)

	vim.cmd('call inputsave()')
	vim.cmd('let g:new_keybinding = input("' .. prompt .. '")')
	vim.cmd('call inputrestore()')

	local new_key = vim.g.new_keybinding
	vim.g.new_keybinding = nil

	if new_key and new_key ~= "" then
		new_key = new_key:gsub("^%s+", ""):gsub("%s+$", "")
		
		if new_key:sub(1, 1) == "<" then
		else
			new_key = "<" .. new_key .. ">"
		end

		config.set_keybinding(action_key, new_key)
		require("refactor.init").setup_keymaps()

		vim.notify(
			("Keybinding changed: %s -> %s"):format(action_label, format_keybinding(new_key)),
			vim.log.levels.INFO
		)
	end
end

return M