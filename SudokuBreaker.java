package fun;

import java.util.*;

public class SudokuBreaker {

	Integer[][] grid; // [r][c]
	Integer[][] prevGrid;
	ArrayList<HashSet<Integer>> numUniverse;
	int currRow = 0;
	int currCol = 0;
	int iterations = 0;
	Integer[] FULLSET = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	Stack<Integer[]> attemptedElements;
	Stack<HashMap<Integer[][], ArrayList<HashSet<Integer>>>> snapshots;

	private void init() { // init variables
		getInput();
		System.out.println("Initial grid:");
		printGrid();
		numUniverse = new ArrayList<HashSet<Integer>>();
		attemptedElements = new Stack<Integer[]>();
		snapshots = new Stack<HashMap<Integer[][], ArrayList<HashSet<Integer>>>>();
		for (int i = 0; i < 81; i++) {
			HashSet<Integer> seed = new HashSet<Integer>();
			if (grid[getCoordinate(i)[0]][getCoordinate(i)[1]] == 0) {
				seed.addAll(Arrays.asList(FULLSET));
			}
			numUniverse.add(new HashSet<Integer>(seed));
		}
	}

	public void run() {
		while (!isFinished()) {
			currRow = 0;
			currCol = -1;
			iterations++;
			solve();
		}
		System.out.printf("Sudoku solved after %d iterations!\n", iterations);
	}

	public boolean isFinished() {
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				if (grid[i][j] == 0) {
					return false;
				}
			}
		}
		return true;
	}

	private void solve() {
		prevGrid = cloneGrid(grid);
		exclusionSolver();
		System.out.printf("iteration %d Exclusion:\n", iterations);
		printGrid();
		outcastSolver();
		System.out.printf("iteration %d Outcast:\n", iterations);
		printGrid();
		if (!isGridChange()) {
			trialAndErrorSolver();
			System.out.println("Using Trial and Error");
		}
	}

	private void exclusionSolver() {
		getNextGap();
		while (currCol != -1) {
			numUniverse.get(getSeqNum(currRow, currCol)).removeAll(
					getRow(currRow));
			numUniverse.get(getSeqNum(currRow, currCol)).removeAll(
					getCol(currCol));
			numUniverse.get(getSeqNum(currRow, currCol)).removeAll(getBlock());
			if (numUniverse.get(getSeqNum(currRow, currCol)).size() == 1) {
				for (Integer n : numUniverse.get(getSeqNum(currRow, currCol))) {
					setGridElementAtCoordinate(currRow, currCol, n);
				}
			}

			else if (numUniverse.get(getSeqNum(currRow, currCol)).size() == 0) {
				System.out.println("reverse Trial and Error");
				reverse();
				return;
			}
			getNextGap();

		}
	}

	private void outcastSolver() {
		// row outcast
		for (int r = 0; r < 9; r++) {
			rowOutcast(r);
		}

		// col outcast
		for (int c = 0; c < 9; c++) {
			colOutcast(c);
		}

		// block outcast
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				blockOutcast(3 * r, 3 * c);
			}
		}
	}

	private void rowOutcast(int row) {
		HashMap<Integer, ArrayList<Integer>> counter = new HashMap<Integer, ArrayList<Integer>>();
		for (int c = 0; c < 9; c++) {
			if (grid[row][c] == 0) {
				for (Integer n : numUniverse.get(getSeqNum(row, c))) {
					// create entry if there isnt one
					if (!counter.keySet().contains(n)) {
						ArrayList<Integer> seqNumList = new ArrayList<Integer>();
						counter.put(n, seqNumList);
					}
					// add seqNum
					counter.get(n).add(getSeqNum(row, c));
				}
			}
		}

		deductElementFromCounter(counter);
	}

	private void colOutcast(int col) {
		HashMap<Integer, ArrayList<Integer>> counter = new HashMap<Integer, ArrayList<Integer>>();
		for (int r = 0; r < 9; r++) {
			if (grid[r][col] == 0) {
				for (Integer n : numUniverse.get(getSeqNum(r, col))) {
					// create entry if there isn't one
					if (!counter.keySet().contains(n)) {
						ArrayList<Integer> seqNumList = new ArrayList<Integer>();
						counter.put(n, seqNumList);
					}
					// add seqNum
					counter.get(n).add(getSeqNum(r, col));
				}
			}
		}
		deductElementFromCounter(counter);
	}

	private void blockOutcast(int startRow, int startCol) {
		HashMap<Integer, ArrayList<Integer>> counter = new HashMap<Integer, ArrayList<Integer>>();
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				if (grid[startRow + r][startCol + c] == 0) {
					for (Integer n : numUniverse.get(getSeqNum(startRow + r,
							startCol + c))) {
						// create entry if there isn't one
						if (!counter.keySet().contains(n)) {
							ArrayList<Integer> seqNumList = new ArrayList<Integer>();
							counter.put(n, seqNumList);
						}
						// add seqNum
						counter.get(n).add(
								getSeqNum(startRow + r, startCol + c));
					}
				}
			}
		}
		deductElementFromCounter(counter);
	}

	private void deductElementFromCounter(
			HashMap<Integer, ArrayList<Integer>> counter) {
		for (Integer n : counter.keySet()) {
			// the outcast
			if (counter.get(n).size() == 1) {
				int[] coordinate = getCoordinate(counter.get(n).get(0));
				setGridElementAtCoordinate(coordinate[0], coordinate[1], n);
//				grid[coordinate[0]][coordinate[1]] = n;
//				cleanNumUniverse(coordinate[0], coordinate[1], n);
			}
		}
	}

	private void trialAndErrorSolver() {
		// add snapshot to stack
		HashMap<Integer[][], ArrayList<HashSet<Integer>>> ss = new HashMap<Integer[][], ArrayList<HashSet<Integer>>>();
		ss.put(cloneGrid(grid), cloneNumUniv(numUniverse));
		snapshots.push(ss);
		// add attempted ele to stack
		Integer[] ae = findElementForTrialAndError(numUniverse);
		attemptedElements.push(ae);
		int row = ae[0] / 9;
		int col = ae[0] % 9;
		setGridElementAtCoordinate(row, col, ae[1]);
	}

		//{seqNum, element}
	private Integer[] findElementForTrialAndError(ArrayList<HashSet<Integer>> myNumUniv) {
		int minSize = 10;
		for (int i = 0; i < 81; i ++) {
			if(myNumUniv.get(i).size() != 0 && myNumUniv.get(i).size() < minSize){
				minSize = myNumUniv.get(i).size();
			}
		}
		Integer[] ret = new Integer[2];
		for (int i = 0; i < 81; i ++){
			if(myNumUniv.get(i).size() == minSize){
				ret[0] = i;
				for (Integer n:myNumUniv.get(i)){
					ret[1] = n;
					break;
				}
			}
		}
		return ret;
	}

	private HashSet<Integer> getRow(int row) {
		HashSet<Integer> rowSet = new HashSet<Integer>();
		for (int c = 0; c < 9; c++) {
			int element = grid[row][c];
			if (element != 0) {
				rowSet.add(element);
			}
		}
		return rowSet;
	}

	private HashSet<Integer> getCol(int col) {
		HashSet<Integer> colSet = new HashSet<Integer>();
		for (int r = 0; r < 9; r++) {
			int element = grid[r][col];
			if (element != 0) {
				colSet.add(element);
			}
		}
		return colSet;
	}

	private HashSet<Integer> getBlock() {
		HashSet<Integer> blockSet = new HashSet<Integer>();
		int startRow = currRow - currRow % 3;
		int startCol = currCol - currCol % 3;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				int element = grid[startRow + i][startCol + j];
				if (element != 0) {
					blockSet.add(element);
				}
			}
		}
		return blockSet;
	}

	private void getNextGap() {
		int[] ret = new int[2];
		int startCol = currCol + 1;
		for (int r = currRow; r < 9; r++) {
			for (int c = startCol; c < 9; c++) {
				if (grid[r][c] == 0) {
					ret[0] = r;
					ret[1] = c;
					currRow = r;
					currCol = c;
					return;
				}
			}
			startCol = 0;
		}
		currRow = 0;
		currCol = -1;
	}

	private int getGridElementFromSeqNum(int seqNum) {
		return grid[getCoordinate(seqNum)[0]][getCoordinate(seqNum)[1]];
	}

	private static int getSeqNum(int r, int c) {
		return r * 9 + c;
	}

	private static int[] getCoordinate(int seqNum) {
		int[] coordinate = new int[2];
		coordinate[0] = seqNum / 9;
		coordinate[1] = seqNum % 9;
		return coordinate;
	}

	private void setGridElementAtCoordinate(int row, int col, Integer element){
		grid[row][col] = element;
		cleanNumUniverse(row, col, element);
	}
	private void cleanNumUniverse(int row, int col, Integer element) {
		// clean row
		for (int c = 0; c < 9; c++) {
			if (c == col) {
				continue;
			}
			if (grid[row][c] == 0) {
				numUniverse.get(getSeqNum(row, c)).remove(element);
			}
		}

		// clean col
		for (int r = 0; r < 9; r++) {
			if (r == row) {
				continue;
			}
			if (grid[r][col] == 0) {
				numUniverse.get(getSeqNum(r, col)).remove(element);
			}
		}

		// clean block
		int startRow = row - row % 3;
		int startCol = col - col % 3;
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				if (grid[startRow + r][startCol + c] == 0) {
					numUniverse.get(getSeqNum(startRow + r, startCol + c))
							.remove(element);
				}
			}
		}
		// clean current block to null
		numUniverse.get(getSeqNum(row, col)).removeAll(Arrays.asList(FULLSET));
	}

	private Integer[][] cloneGrid(Integer[][] g1) {
		Integer[][] ret = new Integer[9][9];
		copyGrid(g1, ret);
		return ret;
	}
	
	private void copyGrid(Integer[][] g1, Integer[][] g2){
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				g2[r][c] = g1[r][c];
			}
		}
	}
	
	private ArrayList<HashSet<Integer>> cloneNumUniv(ArrayList<HashSet<Integer>> numUniv){
		ArrayList<HashSet<Integer>> ret = new ArrayList<HashSet<Integer>>();
		for(HashSet<Integer> univ:numUniv){
			ret.add((HashSet<Integer>)univ.clone());
		}
		return ret;
	}
	

	private boolean isGridChange() {
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				if (grid[r][c] != prevGrid[r][c]) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void reverse(){
		HashMap<Integer[][], ArrayList<HashSet<Integer>>> ss = new HashMap<Integer[][], ArrayList<HashSet<Integer>>>(); 
		try{
		ss = snapshots.pop();
		}
		catch (EmptyStackException e)
		{
			System.err.println("The Sudoku is unsolvable, or you made a mistake typing it in");
			System.exit(0);
		}
		Integer[] ae = attemptedElements.pop();
		for(Integer[][] myGrid : ss.keySet()){
			copyGrid(myGrid, grid);
			ss.get(myGrid).get(ae[0]).remove(ae[1]);
			numUniverse = ss.get(myGrid);
		}
	}

	private void printGrid() {
		// System.out.printf("iteration %d:\n", iterations);
		for (int r = 0; r < 9; r++) {
			if (r % 3 == 0) {
				System.out.println("----------------------");
			}
			for (int c = 0; c < 9; c++) {
				if (c % 3 == 0) {
					System.out.printf("|");
				}
				System.out.printf("%d ", grid[r][c]);
				if (c == 8) {
					System.out.printf("|");
				}
			}
			System.out.print("\n");
		}
		System.out.println("----------------------");
	}

	private void getInput() {
		Integer[][] input = new Integer[9][9];

		Scanner sc = new Scanner(System.in);
		System.out
				.println("Please enter the numbers in a row. Use zero to represent gaps. Do not put anything between numbers. \nHit Enter when finish one row. After finishing all the rows, type q, and then press enter");
		int row = 0;
		while (sc.hasNext()) {
			String rowNum = sc.next();
			if (rowNum.equals("q")) {
				break;
			}
			for (int i = 0; i < 9; i++) {
				input[row][i] = Integer.parseInt(rowNum.substring(i, i + 1));
			}
			row++;
		}
		grid = input;
	}

	public static void main(String args[]) {
		SudokuBreaker SB = new SudokuBreaker();
		SB.init();
		SB.run();
	}
}
