local M = {}

M.defaults = {
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
}

M.config = vim.deepcopy(M.defaults)

M.refactoring_labels = {
	menu = "Open Refactor Menu",
	generate_getters_setters = "Generate Getters/Setters",
	generate_to_string = "Generate toString",
	extract_method = "Extract Method",
	extract_variable = "Extract Variable",
	inline_method = "Inline Method",
	flame_graph = "Flame Graph",
	settings = "Open Settings",
}

function M.setup(user_config)
	user_config = user_config or {}
	
	if user_config.keybindings then
		for key, value in pairs(user_config.keybindings) do
			if M.config.keybindings[key] ~= nil then
				M.config.keybindings[key] = value
			end
		end
	end
end

function M.get_keybinding(action)
	return M.config.keybindings[action] or M.defaults.keybindings[action]
end

function M.set_keybinding(action, keybinding)
	M.config.keybindings[action] = keybinding
end

function M.get_all_keybindings()
	return vim.deepcopy(M.config.keybindings)
end

function M.reset_to_defaults()
	M.config = vim.deepcopy(M.defaults)
end

return M
