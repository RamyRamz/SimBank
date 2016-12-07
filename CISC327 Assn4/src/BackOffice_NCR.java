import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author NCR Studios, November 14th 2016
 * V 1.0 Back Office
 * The Back Office reads the Master Accoutns file and the Merged Transaction Summary File
 * And applies all transactions to the master accoutns to produce the New Master Accounts File and the Valid Accounts List File
 * The Back Office enforces business constraints and produces a failed constraint log on the terminal as it processes transactions
 */


/**
 * The BackOffice Class holds a map of accounts that uses the account number as the key
 * It also holds a queue of all the transactions to be executed.
 * As soon as the transaction is executed, it is popped off the stack.
 * The methods inside this class handle processing each transaction and 
 * uses the account class to store the individual user's account information.
 */
public class BackOffice_NCR {
	
	// Holds all the account objects in a hash map
	// uses account number as the key
	private Map<Integer, Account> accounts;

	// Holds all the merged transactions to be executed
	// in FIFO order
	private Queue<String> transactions;
	
	/**
	 * @param accountsFile
	 * @param transactionsFile
	 * @param accountsFileName
	 * @param masterAccountsFileName
	 * 
	 * constructor for BackOffice
	 * initializes internal data structures and runs methods to import and process data
	 */
	public BackOffice_NCR(String accountsFile, String transactionsFile,
			String accountsFileName, String masterAccountsFileName) {
		accounts = new HashMap<Integer, Account>();
		transactions = new LinkedList<String>();

		initialize(accountsFile, transactionsFile);
		processTransactions(accountsFileName, masterAccountsFileName);
	}
	
	
//-----------------------Office Methods----------------------	
	
	/**
	 * @param accountNum
	 * @param balance
	 * @param name
	 * 
	 * adds new account to accounts HashMap, brand new accounts are
	 * created with a balance of 0
	 * inputs are assumed to be valid
	 */
	private void create(int accountNum, int balance, String name) {
		if (!accounts.containsKey(accountNum)) 
			accounts.put(accountNum, new Account(accountNum, balance, name));
		else 
			System.err.println("Could not create account with number " + accountNum);
		
	}
	
	/**
	 * @param accountNum
	 * @param name
	 * 
	 * deletes an account if conditions are met
	 * inputs are assumed to be valid
	 */
	private void delete(int accountNum, String name) {
		if (accounts.get(accountNum) != null &&			//account exists
			accounts.get(accountNum).getBalance() == 0 &&	//balance is 0
			accounts.get(accountNum).getName().equals(name)) { //name matches
			accounts.remove(accountNum);
		} else {
			System.err.println("delete could not be processed on account " + accountNum);
		}
	}
	
	/**
	 * @param accountNum
	 * @param value
	 * @return success of method
	 * 
	 * attempts to increase the balance of the specified account by the given amount
	 * inputs are assumed to be valid
	 */
	private boolean deposit(int accountNum, int value) {
		if (accounts.get(accountNum) != null) {
			boolean success = accounts.get(accountNum).increaseBalance(value);
			if (!success)
				System.err.println("maximum balance exceeded");
			return success;
		} else {
			System.err.println("Account " + accountNum + " does not exist");
			return false;
		}
	}
	
	/**
	 * @param accountNum
	 * @param value
	 * @return
	 * 
	 * attempts to decrease the balance of the specified account by the given amount
	 * inputs are assumed to be valid
	 */
	private boolean withdraw(int accountNum, int value) {
		if (accounts.get(accountNum) != null) {
			System.err.println("\nconditional #1 passed");
			boolean success = accounts.get(accountNum).decreaseBalance(value); //returns success of command
			if (!success){
				System.err.println("conditional #2 passed");
				System.err.println("value exceeds account balance\n");
			}
			else{
				System.err.println("conditional #2 failed");
			}
			return success;
		} else {
			System.err.println("conditional #1 failed");
			System.err.println("Account " + accountNum + " does not exist\n");
			return false; // never reached, fatal kills program
		}
	}
	
	/**
	 * @param fromAccount - account number of the account to transfer money from
	 * @param toAccount - account number of the account to transfer money to
	 * @param value - amount to transfer
	 * 
	 * runs deposit and withdraw on the specified accounts to simulate a transfer
	 * inputs are assumed to be valid
	 */
	private void transfer(int fromAccount, int toAccount, int value){
		if (withdraw(fromAccount, value) && deposit(toAccount, value)); //transfer succeeded
		else if (withdraw(fromAccount, value) && !deposit(toAccount, value)) //withdraw successful, deposit fails
			deposit(fromAccount, value);
		else;//withdraw fails, do nothing
	}
	
	/**
	 * @param accountsFileName - output name of accounts file for front end
	 * @param MAFName - output name of master accounts file
	 * 
	 * writes account information to the Master Accounts File and generates the accounts
	 * file used by the front end of the bank, clears internal data structures
	 */
	private void endSession(String accountsFileName, String MAFName) {
		writeAccountsFile(accountsFileName);
		writeMasterAccountsFile(MAFName);
		//accounts.clear();
		transactions.clear();// should already be empty, but just in case
		System.out.println("END OF SESSION");
		//System.exit(0);
	}
//-----------------------Helper methods----------------------
		
	/** displays fatal error message
	 *  exits program
	 */
	private void fatal() {
		System.err.println("Fatal Error. Program Exiting.");
		System.exit(0);
	}
		
	/**
	 * @param accountsFile - name of master accounts file
	 * @param transactionFile - name of merged transaction summary file
	 * 
	 * runs methods to read data from master accounts file and transaction 
	 * summary file into internal data structures
	 */
	private void initialize(String accountsFile, String transactionFile) {
		readAccountsFile(accountsFile);// read master accounts file
		readTransactionFile(transactionFile);
	}
	
	/**
	 * @param accountsFileName
	 * @param MAFName
	 * 
	 * Executes all the transactions retrieved from the merged transaction summary file
	 * parameters are passed to endSession(String,String) when it is called
	 */
	private void processTransactions(String accountsFileName, String MAFName) {
		while (!transactions.isEmpty()) {
			String[] args = transactions.remove().split(" ", 5);
			// args[0] = command
			// args[1] = first account number
			// args[2] = second account number
			// args[3] = money value
			// args[4] = account name
			int aNum1 = 0, aNum2 = 0, money = 0;
			String cmd = "", name = "***";

			validateTransaction(args);// kills program if given bad input

			aNum1 = Integer.parseInt(args[1]);
			aNum2 = Integer.parseInt(args[2]);
			money = Integer.parseInt(args[3]);
			cmd = args[0];
			name = args[4].trim();

			switch (cmd) {
			case "CR":
				create(aNum1, money, name);
				break;
			case "DL":
				delete(aNum1, name);
				break;
			case "DE":
				deposit(aNum1, money);
				break;
			case "WD":
				withdraw(aNum1, money);
				break;
			case "TR":
				transfer(aNum1, aNum2, money);
				break;
			case "ES":
				endSession(accountsFileName, MAFName);
				break;
			default:
				fatal();
			}
		}
	}
	
	/**
	 * @param transaction
	 * 
	 * ensures that the lines in the transaction summary file are legal input
	 * before they are passed to the various methods in the program
	 */
	private void validateTransaction(String[] transaction) {
		try {
			// command code
			if (transaction[0].length() != 2)
				fatal();

			try {
				// first account number
				if (Integer.parseInt(transaction[1]) != 0
						&& (Integer.parseInt(transaction[1]) < 10000000 || Integer
								.parseInt(transaction[1]) > 99999999))
					fatal();
				// second account number
				if (Integer.parseInt(transaction[2]) != 0
						&& (Integer.parseInt(transaction[2]) < 10000000 || Integer
								.parseInt(transaction[2]) > 99999999))
					fatal();
				// money
				if (Integer.parseInt(transaction[3]) != 0
						&& ((transaction[3].length() < 3
								|| Integer.parseInt(transaction[3]) < 1 || Integer
								.parseInt(transaction[3]) > 99999999)))
					fatal();
			} catch (NumberFormatException e) {
				fatal();
			}
			// account name
			if (transaction[4].trim().length() < 3
					|| transaction[4].trim().length() > 30)
				fatal();
		} catch (ArrayIndexOutOfBoundsException ex) {
			fatal();
		}
	}
	
	/**
	 * @param filename
	 * @return filename
	 * 
	 * ensures that the filetype of the accounts file and master
	 * accounts file is .txt
	 */
	private String fixFileName(String filename) {
		String temp = filename.substring(filename.length() - 4);
		if (!temp.equals(".txt")) {
			return filename + ".txt";
		}
		return filename;// test.txt
	}
	
	/**
	 * @param accountsFile - name of the master accounts file
	 * 
	 * populates the accounts map with information from the lines of the master
	 * accounts file
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
					String[] line = cur.split(" ", 3);
					int num = Integer.parseInt(line[0]), 
					balance = Integer.parseInt(line[1]);
					String name = line[2];
					accounts.put(num, new Account(num, balance, name));

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
	
	/**
	 * @param transactionFile - name of the merged transaction summary file
	 * 
	 * reads the lines of the merged transaction summary file into a 
	 * queue of strings
	 */
	private void readTransactionFile(String transactionFile) {
		File inFile = new File(transactionFile);
		BufferedReader br = null;
		try {
			String line;
			br = new BufferedReader(new FileReader(inFile));

			while ((line = br.readLine()) != null) {
				transactions.add(line);
			}
		} catch (IOException e) {
			System.err.println("Could not read the transaction summary file.");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				System.out
						.println("Could not read the transaction summary file.");
			}
		}
	}
	
	/**
	 * @param fileName - name of accounts file
	 * 
	 * sorts account numbers in ascending order and writes 
	 * account numbers to a text file
	 */
	private void writeAccountsFile(String fileName) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		PrintWriter out = null;
		try {
			out = new PrintWriter(fixFileName(fileName));

			for (int accountNum : accounts.keySet()) {
				temp.add(accounts.get(accountNum).getAccountNum());
			}

			Collections.sort(temp);
			for (int number : temp)
				out.write(number + "\n");
			out.write("00000000");
		} catch (FileNotFoundException e) {
			System.out.println("Could not write to file.");
		} finally {
			out.close();
		}
	}
	
	/**
	 * @param fileName - name of output .txt file
	 * 
	 * writes account number, balance, and name of account holder to 
	 * a text file in ascending order
	 */
	private void writeMasterAccountsFile(String fileName) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(fixFileName(fileName));
			ArrayList<String> temp = new ArrayList<String>();
			for (int accountNum : accounts.keySet()) {
				temp.add(accounts.get(accountNum).toString());
			}
			Collections.sort(temp);
			for (String str : temp)
				out.write(str + '\n');

		} catch (FileNotFoundException e) {
			System.out.println("Could not write to file.");
		} finally {
			out.close();
		}
	}
//----------------------------MAIN--------------------------
	/**
	 * @param args - 
	 * args[0] = master accounts file name
	 * args[1] = Merged Transaction Summary File name
	 * args[2] = name of outputted accounts file
	 * args[3] = name of outputted Master Accounts File
	 * 
	 * main method of BackOffice
	 * checks arguments for validity then passes them to the constructor
	 */
	public static void main(String[] args) {
		String accts = null, 
			   transSumFile = null, 
			   accountsFileName = null, 
			   masterAccountsFileName = null;
		try {
			accts = args[0];
			transSumFile = args[1];
			try {
				accountsFileName = args[2];
				masterAccountsFileName = args[3];
			} catch (ArrayIndexOutOfBoundsException ex) {
				accountsFileName = "accounts.txt";
				masterAccountsFileName = "MAF.txt"; // master accounts file
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Invalid arguments.");
			System.exit(0);
		}
		//new BackOffice_NCR(accts, transSumFile, accountsFileName, masterAccountsFileName);
		
		//need to manually set MAF to starting settings for withdraw tests
		withdrawTestSuccess();
		withdrawTestFailNoAccount();
		withdrawTestFailLowBalance();
		
		testCreateSuccess();
		testCreateSuccessZero();
		testCreateFailExists();
		testCreateFailNeg();
	}
	
//----------------------------UNIT TESTING----------------------------
	
	static void withdrawTestSuccess(){
		String inMTSF = "withdrawTestSuccessfulMTSF.txt",
			   inMAF = "withdrawTestSuccessfulMAF.txt",
			   outAcct = "accounts5.txt",
			   outMAF = "MAF5.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		final int accountNumValid = 12345678;
//		int withdraw1 = 100000;
//		
//		System.out.println("Withdrawal test - Succesful: ");
//		System.out.println("Account Number: " + accountNumValid + "\nAccount Balance: " + accounts.get(accountNumValid).getBalance() + "\nAmount To Withdraw: " + withdraw1);
//		System.out.println("Withdraw Successful: " + withdraw(accountNumValid, withdraw1) + "\n");
	}
	
	static void withdrawTestFailNoAccount(){
		String inMTSF = "../TEST_INPUTS/Inputs_WD/withdrawTestFailNoAccountMTSF.txt",
			   inMAF = "../TEST_INPUTS/Inputs_WD/withdrawTestFailNoAccountMAF.txt",
			   outAcct = "accounts6.txt",
			   outMAF = "MAF6.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		final int accountNumInvalid = 12345679;
//		int withdraw2 = 200000;
//		
//		System.out.println("Withdrawal test - Non-existent account");
//		try{
//			int acctBal = accounts.get(accountNumInvalid).getBalance();
//			System.out.println("Account Number: " + accountNumInvalid + "\nAccount Balance: " + acctBal + "\nAmount To Withdraw: " + withdraw2);
//		}catch(NullPointerException e){
//			System.out.println("Account Number: " + accountNumInvalid + "\nAccount Balance: null\nAmount To Withdraw: " + withdraw2);
//		}
//		System.out.println("Withdraw Successful: " + withdraw(accountNumInvalid, withdraw2) + "\n");
	}
	
	static void withdrawTestFailLowBalance(){
		String inMTSF = "../TEST_INPUTS/Inputs_WD/withdrawTestFailLowBalanceMTSF.txt",
			   inMAF = "../TEST_INPUTS/Inputs_WD/withdrawTestFailLowBalanceMAF.txt",
			   outAcct = "accounts7.txt",
			   outMAF = "MAF7.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		final int accountNumValid = 12345678;
//		int withdraw3 = 200000;
//		System.out.println("Withdrawal test - balance > withdraw value");
//		System.out.println("Account Number: " + accountNumValid + "\nAccount Balance: " + accounts.get(accountNumValid).getBalance() + "\nAmount To Withdraw: " + withdraw3);
//		System.out.println("Withdraw Successful: " + withdraw(accountNumValid, withdraw3));
	}
	
	static void testCreateSuccess(){
		String inMTSF = "../TEST_INPUTS/Inputs_CR/MTSF1.txt",
			   inMAF = "../TEST_INPUTS/Inputs_CR/MAF1.txt",
			   outAcct = "accounts1.txt",
			   outMAF = "MAF1.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		String accountName = "John Doe";
//		int accountNum = 12341234;
//		int accountBal = 1;
//		System.out.println("Creation test, non-zero balance - Successful: ");
//		System.out.println("Account Number: " + accountNum + "\nAccount Balance: " + accountBal + "\nAccount Name: " + accountName);
//		create(accountNum,accountBal,accountName);
//		System.out.println("Account creation successful: " + (accounts.get(accountNum) != null) + "\n");
//		accounts.clear();
	}
	
	static void testCreateSuccessZero(){
		String inMTSF = "../TEST_INPUTS/Inputs_CR/MTSF2.txt",
			   inMAF = "../TEST_INPUTS/Inputs_CR/MAF2.txt",
			   outAcct = "accounts2.txt",
			   outMAF = "MAF2.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		String accountName = "John Doe";
//		int accountNum = 12341234;	
//		int accountBal = 0;
//		System.out.println("Creation test, zero balance - Successful: ");
//		System.out.println("Account Number: " + accountNum + "\nAccount Balance: " + accountBal + "\nAccount Name: " + accountName);
//		create(accountNum,accountBal,accountName);
//		System.out.println("Account creation successful: " + (accounts.get(accountNum) != null) + "\n");
//		accounts.clear();
	}
	
	static void testCreateFailNeg(){
		String inMTSF = "../TEST_INPUTS/Inputs_CR/MTSF3.txt",
			   inMAF = "../TEST_INPUTS/Inputs_CR/MAF3.txt",
			   outAcct = "accounts3.txt",
			   outMAF = "MAF3.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		String accountName = "John Doe";
//		int accountNum = 12341234;	
//		int accountBal = -1;
//		System.out.println("Creation test, negative balance - Unsuccessful: ");
//		System.out.println("Account Number: " + accountNum + "\nAccount Balance: " + accountBal + "\nAccount Name: " + accountName);
//		create(accountNum,accountBal,accountName);
//		System.out.println("Account creation successful: " + (accounts.get(accountNum) != null) + "\n");
//		accounts.clear();
	}
	
	static void testCreateFailExists(){
		String inMTSF = "../TEST_INPUTS/Inputs_CR/MTSF4.txt",
			   inMAF = "../TEST_INPUTS/Inputs_CR/MAF4.txt",
			   outAcct = "accounts4.txt",
			   outMAF = "MAF4.txt";
		new BackOffice_NCR(inMAF,inMTSF,outAcct,outMAF);
		
//		String accountName = "John Doe";
//		int accountNum = 12341234;	
//		int accountBal = 1;
//		create(accountNum,accountBal,accountName);
//		System.out.println("Creation test, account already exists - Unsuccessful: ");
//		System.out.println("Account Number: " + accountNum + "\nAccount Balance: " + accountBal + "\nAccount Name: " + accountName);
//		create(accountNum,accountBal,accountName);
//		System.out.println("Account creation successful: " + !(accounts.get(accountNum) != null) + "\n");
//		accounts.clear();
	}

}





















