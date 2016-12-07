
/**
 * Simple Interactive Banking System (SimBank) ATM banking transaction acceptor
 * for simple ATM style banking transactions. Reads in a list of valid account
 * numbers, processes a stream of transactions one at a time, and writes out a
 * summary file of transactions at the end of the day.
 * 
 * SimBank takes one input file, an accounts file and outputs one file, the 
 * transaction summary file
 * 
 * SimBank requires two arguments when run, the name of the accounts file first, 
 * then the name of the transaction summary file to be generated on logout
 * 
 * The program is intended to be run by using std input as a way of navigating
 * through the program menus and features. However, during testing std input is 
 * simulated using Bash scripts utilizing pipes to provide input from text files
 * 
 * @author NCR Studios 
 * 		   Ramy Ayash - 10130200 
 * 		   Nick Purdye - 10146598 
 * 		   Corey Maikawa - 10149134
 * 
 * @since Oct-12-2016
 * @version 1.0
 */

import java.io.*;
import java.util.*;

public class FrontEnd_NCR {
	private Scanner reader = new Scanner(System.in);
	private boolean bAgentMode, // true for agent, false for ATM
			bLoggedIn = false;

	// output messages
	private final String INVALID = "Invalid Input.", NOACCOUNT = "Account does not exist.",
			YESACCOUNT = "Account already exists.", LIMIT = "Daily transaction limit exceeded.",
			PERMS = "You do not have authorization to run this command.";

	private String transSumFileName, accountsFile;

	// stores transaction strings, written to file when logout() is called
	private ArrayList<String> transactions;

	// stores account numbers
	private ArrayList<Integer> accounts;

	// stores data about the amount withdrawn from an account during a session
	// via withdraw or transfer commands
	private Map<Integer, Integer> withdrawAmounts;

	// constructor, runs on program start
	public FrontEnd_NCR(String accountsFile, String transSumFileName) {
		transactions = new ArrayList<String>();
		accounts = new ArrayList<Integer>();
		withdrawAmounts = new HashMap<Integer, Integer>();
		this.transSumFileName = transSumFileName;
		this.accountsFile = accountsFile;
		frontEnd();
	}

	// -----------------------MAIN PROGRAM LOOP-------------------------------------

	/**
	 * after login, waits for user input, runs methods based on input
	 */
	private void frontEnd() {
		while (true) {
			login();

			while (bLoggedIn) {
				System.out.print("enter command: ");
				switch (reader.nextLine().trim().toLowerCase()) {
				case "create":
					create();
					break;
				case "delete":
					delete();
					break;
				case "deposit":
					deposit();
					break;
				case "withdraw":
					withdraw();
					break;
				case "transfer":
					transfer();
					break;
				case "logout":
					logout();
					break;
				default:
					System.out.println(INVALID);
					break;
				}
			}
		}
	}

	// -----------------------------COMMAND METHODS-------------------------------

	/**
	 * remains in loop until user inputs "login" followed by "atm" or "agent"
	 * set logged in flag (bLoggedIn) to true on exit
	 */
	private void login() {
		while (true) {
			System.out.println("Please login to start session");
			String input = reader.nextLine().trim().toLowerCase();

			if (input.equals("login") && loginType()) {
				bLoggedIn = true;
				readAccountsFile(accountsFile);
				return;
			} else {
				System.out.println(INVALID);
			}
		}
	}

	/**
	 * creates and outputs transaction summary file (transactions.txt) changes
	 * logged in flag (bLoggedIn) to false
	 */
	private void logout() {
		String transInfo = "ES 00000000 00000000 000 ***";
		transactions.add(transInfo);
		System.out.println("Thank you for using SimBank today");
		bLoggedIn = false;
		writeTransactionFile();
		accounts.clear();
		transactions.clear();
		withdrawAmounts.clear();
	}

	/**
	 * logs a create transaction when run in "agent" mode adds created account
	 * to the "accounts" arrayList
	 */
	private void create() {
		if (bAgentMode) {
			boolean bNameOK, bNumOK;
			String transInfo = "CR ";
			System.out.print("What is the new account number: ");
			int inputNum = 0;
			String accountName = "default";
			String input = takeNumber();
			inputNum = Integer.parseInt(input);
			bNumOK = (validateAccount(input) && accountExists(inputNum, false) && inputNum > 0);

			if (bNumOK) {
				transInfo += input + " ";
				transInfo += "00000000 000 ";

				accountName = takeName();
				bNameOK = validateName(accountName);

				if (bNameOK) {
					transInfo += accountName;
					transactions.add(transInfo);
					accounts.add(inputNum);
					System.out.println("Account created");
				}
			}
		} else // atm case
			System.out.println(PERMS);

	}

	/**
	 * logs a delete transaction when run in "agent" mode removes the specified
	 * account from the "accounts" arrayList
	 */
	private void delete() {
		if (bAgentMode) {
			boolean bNumOK, bNameOK;
			String transInfo = "DL ";
			System.out.print("Account number to delete: ");
			int inputNum = 0;
			String accountName = "default";
			String input = takeNumber();
			inputNum = Integer.parseInt(input);
			bNumOK = (validateAccount(input) && accountExists(inputNum, true) && inputNum > 0);

			if (bNumOK) {
				transInfo += input + " ";
				transInfo += "00000000 000 "; // fill unused fields in transaction info
				accountName = takeName();
				bNameOK = validateName(accountName);

				if (bNameOK) {
					transInfo += accountName;
					transactions.add(transInfo);
					accounts.remove(accounts.indexOf(inputNum));
					System.out.println("Account deleted");
				}
			}
		} else
			System.out.println(PERMS);
	}

	/**
	 * logs a deposit transaction to a valid account
	 */
	private void deposit() {
		boolean bNumOK, bValOK;
		String transInfo = "DE ";
		System.out.print("Account number to deposit into: ");
		int accountNum = 0, depValue = 0;

		String input = takeNumber();
		accountNum = Integer.parseInt(input);
		bNumOK = (validateAccount(input) && accountExists(accountNum, true));
		if (bNumOK) {
			transInfo += input + " ";
			transInfo += "00000000 ";

			System.out.print("Amount to deposit: ");
			String dVal = takeNumber();
			depValue = Integer.parseInt(dVal);
			bValOK = validateMoney(depValue);
			if (bValOK) {
				transInfo += dVal + " ";
				transInfo += "***";
				transactions.add(transInfo);
			}
		}
	}

	/**
	 * logs a withdraw transaction from a valid account updates the value
	 * associated with that account number in the "withdrawAmounts" Map
	 */
	private void withdraw() {
		boolean bNumOK, bValOK;
		String transInfo = "WD ";
		int accountNum = 0, withdrawValue = 0;

		System.out.print("Account number to withdraw from: ");
		String input = takeNumber();
		accountNum = Integer.parseInt(input);
		bNumOK = (validateAccount(input) && accountExists(accountNum, true));
		if (bNumOK) {
			transInfo += accountNum + " 00000000 ";
			System.out.print("Amount to withdraw: ");
			withdrawValue = Integer.parseInt(takeNumber());
			bValOK = validateMoney(withdrawValue);

			if (bValOK && withdrawAmounts.containsKey(accountNum)) {
				if (withdrawAmounts.get(accountNum) + withdrawValue <= 100000) {
					int temp = withdrawAmounts.get(accountNum);
					withdrawAmounts.replace(accountNum, temp + withdrawValue);
				} else {
					bValOK = false; // transaction limit exceeded
					System.out.println(LIMIT);
				}
			} else
				withdrawAmounts.put(accountNum, withdrawValue);

			if (bValOK) {
				transInfo += withdrawValue + " ***";
				transactions.add(transInfo);
			}
		}
	}

	/**
	 * logs a transfer transaction between two valid accounts
	 */
	private void transfer() {
		boolean bFromOK, bToOK, bValOK;
		String transInfo = "TR ";
		int fromAccount, toAccount, amount;

		System.out.print("Account number to transfer from: ");
		String from = takeNumber();
		fromAccount = Integer.parseInt(from);
		bFromOK = validateAccount(from) && accountExists(fromAccount, true);

		if (bFromOK) {
			System.out.print("Account number to transfer to: ");
			String to = takeNumber();
			toAccount = Integer.parseInt(to);
			bToOK = validateAccount(to) && accountExists(toAccount, true);

			if (bFromOK && bToOK) {
				transInfo += from + " " + to + " ";

				System.out.print("Enter the amount to transfer: ");
				String muns = takeNumber();
				amount = Integer.parseInt(muns);
				bValOK = validateMoney(amount);

				if (bValOK && withdrawAmounts.containsKey(fromAccount)) {
					if (withdrawAmounts.get(fromAccount) + amount <= 100000) {
						int temp = withdrawAmounts.get(fromAccount);
						withdrawAmounts.replace(fromAccount, temp + amount);
					} else {
						bValOK = false; // transaction limit exceeded
						System.out.println(LIMIT);
					}
				} else
					withdrawAmounts.put(fromAccount, amount);

				if (bValOK) {
					transInfo += amount + " ***";
					transactions.add(transInfo);
				}
			}
		}
	}

	// -----------------------------HELPER METHODS------------------------------------

	/**
	 * after login command, waits for either "atm" or "agent" as input if
	 * anything else is entered, returns false
	 * 
	 * @return success of login as a boolean
	 */
	private boolean loginType() {
		System.out.println("Are you an ATM or an Agent today?");
		String input = reader.nextLine().trim().toLowerCase();
		if (input.equals("atm")) {
			bAgentMode = false;
			return true;
		} else if (input.equals("agent")) {
			bAgentMode = true;
			return true;
		} else 
			return false;
	}

	/**
	 * returns a string containing an integer, negative values indicate that the
	 * input was invalid
	 * 
	 * @return integer value
	 */
	private String takeNumber() {
		if (!reader.hasNextInt()) {
			reader.nextLine();
			return "-1";
		} else {
			String input = reader.nextLine();
			return input;
		}

	}

	/**
	 * returns a string containing input from the user
	 * 
	 * @return String containing user input
	 */
	private String takeName() {
		System.out.print("What is the account holder's name: ");
		String accountName = reader.nextLine().trim();
		return accountName;
	}

	/**
	 * this method takes an account number (accountNum) and a boolean flag that
	 * modifies our expected output based on the state of the flag
	 * 
	 * @param accountNum
	 * @param bMode
	 * @return XNOR(accountNum membership in accounts, bMode)
	 */
	private boolean accountExists(int accountNum, boolean bMode) {
		if (accounts.contains(accountNum) == bMode)
			return true;
		else if (!accounts.contains(accountNum) && bMode)
			System.out.println(NOACCOUNT);
		else if (accounts.contains(accountNum) && !bMode)
			System.out.println(YESACCOUNT);
		return false;

	}

	/**
	 * @param num
	 * @return num is 8 chars long and does not start with 0
	 */
	private boolean validateAccount(String num) {
		if (!(num.toCharArray()[0] == '0' || num.length() != 8)) 
			return true;
		else
			System.out.println(INVALID);
		return false;
	}

	/**
	 * validates input account names meet parameters outlined in requirements
	 * 
	 * @param name
	 * @return boolean(name has length between 3 and 30 inclusive and is
	 *         alphanumeric)
	 */
	private boolean validateName(String name) {
		String allowedChars = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ";
		if (name.length() >= 30 || name.length() < 3) {
			System.out.println(INVALID);
			return false;
		}
		for (char c : name.toCharArray()) {
			if (allowedChars.indexOf(c) == -1) {
				System.out.println(INVALID);
				return false;
			}
		}
		return true;
	}

	/**
	 * Ensures that money amounts (in cents) are within the parameters outlined
	 * in the requirements
	 * 
	 * @param num
	 * @return number less than 9 digits long for agents and less than or equal
	 *         to $1000 for atm
	 */
	private boolean validateMoney(int num) {
		if (num < 0)
			return false;
		if (bAgentMode)
			return (num < 100000000);
		else
			return (num <= 100000);
	}

	/**
	 * Writes all elements in ArrayList 'transactions' to individual lines in a
	 * new text file (transactions.txt)
	 */
	private void writeTransactionFile() {
		PrintWriter out = null;
		try {
			if (transSumFileName == null || transSumFileName.length() < 5) {// "X.txt".length() == 5
				if (transSumFileName.substring(transSumFileName.length() - 4, transSumFileName.length()) == ".txt")
					out = new PrintWriter("transactions.txt");
				else
					out = new PrintWriter(transSumFileName + ".txt");
			} else {
				out = new PrintWriter(transSumFileName);
			}

			for (String s : transactions) {
				out.write(s + "\n");
			}
		} catch (FileNotFoundException e) {
			System.out.println("Could not write to file.");
		} finally {
			out.close();
		}
	}

	/**
	 * reads each line in file specified by parameter 'accountsFile' into a new
	 * element in ArrayList 'accounts'
	 * 
	 * @param accountsFile
	 */
	private void readAccountsFile(String accountsFile) {
		final String ACCT = "Could not load accounts file.";
		File inFile = new File(accountsFile);
		BufferedReader br = null;
		try {
			String cur;
			br = new BufferedReader(new FileReader(inFile));

			while ((cur = br.readLine()) != null) {
				try {
					int acct = Integer.parseInt(cur);
					accounts.add(acct);
				} catch (NumberFormatException e) {
					System.out.println(ACCT);
					System.exit(0);
				}
			}
		} catch (IOException e) {
			System.out.println(ACCT);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				System.out.println(ACCT);
			}
		}
	}

	// ----------------------------MAIN-------------------------------------------

	/**
	 * args[0] is the name and path of the accounts file that is read into the
	 * front end to populate the ArrayList containing account numbers
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String accts = null;
		String transSumFile = null;
		try {
			accts = args[0];
			transSumFile = args[1];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Invalid arguments.");
			System.exit(0);
		}
		new FrontEnd_NCR(accts, transSumFile);
	}

}