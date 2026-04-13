local Menu = require("nui.menu")
local Popup = require("nui.popup")
local config = require("refactor.config")

local M = {}

local settings_popup = nil

local actions_list = {
	{ key = "menu", label = "Open Refactor Menu" },
	{ key = "generate_getters_setters", label = "Generate Getters/Setters" },
	{ key = "generate_to_string", label = "Generate toString" },
	{ key = "extract_method", label = "Extract Method" },
	{ key = "extract_variable", label = "Extract Variable" },
	{ key = "inline_method", label = "Inline Method" },
	{ key = "flame_graph", label = "Flame Graph" },
	{ key = "settings", label = "Open Settings" },
}

local function format_keybinding(key)
	if not key then return "Not set" end
	return key
end

function M.show_settings()
	if settings_popup then
		pcall(function()
			if settings_popup:is_mounted() then
				settings_popup:unmount()
			end
		end)
	end

	settings_popup = Popup({
		position = "50%",
		size = {
			width = 50,
			height = 16,
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
	})

	settings_popup:mount()

	local lines = {
		"  Configure Keybindings",
		"",
	}

	for i, action in ipairs(actions_list) do
		local key = config.get_keybinding(action.key)
		lines[#lines + 1] = ("  %d. %-30s %s"):format(i, action.label, format_keybinding(key))
	end

	lines[#lines + 1] = ""
	lines[#lines + 1] = "  Press number key to change that keybinding"
	lines[#lines + 1] = "  Press q or Esc to close"

	vim.api.nvim_buf_set_lines(settings_popup.bufnr, 0, -1, false, lines)
	vim.bo[settings_popup.bufnr].modifiable = false

	settings_popup:map("n", "q", function()
		M.close_settings()
	end)

	settings_popup:map("n", "<Esc>", function()
		M.close_settings()
	end)

	for i, action in ipairs(actions_list) do
		settings_popup:map("n", tostring(i), function()
			M.prompt_for_keybinding(action.key, action.label)
		end)
	end

	vim.api.nvim_set_current_win(settings_popup.winid)
end

function M.prompt_for_keybinding(action_key, action_label)
	M.close_settings()

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
			-- Key already starts with <, keep it as-is
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

function M.close_settings()
	if settings_popup then
		pcall(function()
			if settings_popup:is_mounted() then
				settings_popup:unmount()
			end
		end)
		settings_popup = nil
	end
end

return M
