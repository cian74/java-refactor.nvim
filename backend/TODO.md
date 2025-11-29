TODO:

generate getters setters method:

only generate if it doesnt exist

extract method:

    turn this:

    int x = m + n - 1 + (m * n);

    to this:

	private int add() {
		return m + n - 1 + (m * n);
	}

-need to send code highlighted by cursor
-wrap with a private function
-give user option to name it

