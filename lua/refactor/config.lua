local M = {}

local config_dir = vim.fn.stdpath("data") .. "/refactor/"
local config_file = config_dir .. "config.json"

M.defaults = {
	keybindings = {
		menu = "<leader>jf",
		generate_getters_setters = "<leader>gg",
		generate_to_string = "<leader>gt",
		extract_method = "<leader>er",
		extract_variable = "<leader>ev",
		inline_method = "<leader>im",
		encapsulate_field = "<leader>ef",
		rename = "<leader>rn",
		pull_push = "<leader>pp",
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
	encapsulate_field = "Encapsulate Field",
	rename = "Rename",
	pull_push = "Pull/Push",
	flame_graph = "Flame Graph",
	settings = "Open Settings",
}

function M.save_config()
	vim.fn.mkdir(config_dir, "p")
	local file = io.open(config_file, "w")
	if file then
		local json = vim.fn.json_encode({ keybindings = M.config.keybindings })
		file:write(json)
		file:close()
	end
end

function M.load_config()
	local file = io.open(config_file, "r")
	if file then
		local content = file:read("*all")
		file:close()
		local ok, data = pcall(vim.fn.json_decode, content)
		if ok and data and data.keybindings then
			for k, v in pairs(data.keybindings) do
				if M.defaults.keybindings[k] then
					M.config.keybindings[k] = v
				end
			end
		end
	end
end

function M.setup(user_config)
	M.load_config()
	
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
	M.save_config()
end

function M.get_all_keybindings()
	return vim.deepcopy(M.config.keybindings)
end

function M.reset_to_defaults()
	M.config = vim.deepcopy(M.defaults)
	M.save_config()
end

return M
