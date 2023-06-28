package bankAccount;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankAccountContoller {

	@Autowired
	private BankAccountRepository repo;

	@Autowired
	private AccountCurrenciesRepository repoAcc;

	@Autowired
	private UserServiceProxy proxy;

	@GetMapping("/bank-account/accounts")
	public List<BankAccount> getAllAccounts() {
		return repo.findAll();
	}

	@PostMapping("/bank-account/account")
	public ResponseEntity<?> createAccount(@RequestBody BankAccount account) {
		if (repo.existsById(account.getAccountID())) {
			String errorMessage = "Account with passed id already exists.";
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
		} else {
			if (!repo.existsByEmail(account.getEmail())) {
				Boolean emailUser = proxy.getUser(account.getEmail()); // provera da li u bazi za korisnike postoji
																		// korisnik za koji se kreira racun tj da li
																		// postoji email kor koji ce biti pridodat
																		// bankovnom r
				if (emailUser.equals(false)) {
					String errorMessage = "User with email doesn't exist.";
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
				} else {
					String roleUser = proxy.getUsersRole(account.getEmail()); // ako postoji korinik proverava se da li
																				// je korisnik USER
					if (!roleUser.equals("USER")) {
						String errorMessage = "User doesn't have role 'USER'.";
						return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
					} else {
						List<AccountCurrencies> savedCurrencies = new ArrayList<>();
						BankAccount createdAccount = repo.save(account);
						for (AccountCurrencies currency : account.getCurrencies()) {
							currency.setBankAccount(createdAccount);
							AccountCurrencies savedCurrency = repoAcc.save(currency);
							savedCurrencies.add(savedCurrency);
						}
						createdAccount.setCurrencies(savedCurrencies);
						return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
					}
				}
			} else {
				String errorMessage = "This user already have an bank account.";
				return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
			}
		}
	}

	@PutMapping("/bank-account/account/{accountID}")
	public ResponseEntity<?> updateAccount(@PathVariable long accountID, @RequestBody BankAccount updatedAccount) {
		BankAccount existingAccount = repo.findById(accountID).orElse(null);
		if (existingAccount == null) {
			String errorMessage = "The account with passed id doesn't exists.";
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
		} else {
			if (existingAccount.getEmail().equals(updatedAccount.getEmail())) {
				for (AccountCurrencies currency : updatedAccount.getCurrencies()) {
					currency.setBankAccount(existingAccount);
					repoAcc.save(currency);
				}
				BankAccount updatedBankAccount = repo.save(updatedAccount);
				return ResponseEntity.ok(updatedBankAccount);
			} else {
				String errorMessage = "You can't update account email.";
				return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);

			}
		}
	}

	@DeleteMapping("/bank-account/account/{email}")
	public void deleteAccount(@PathVariable String email) {
		BankAccount bankAccount = repo.findByEmail(email);
		if (bankAccount != null) {
			repo.delete(bankAccount);
		}
	}

	@GetMapping("/bank-account/{email}/{currencyFrom}")
	public Double getUserCurrencyAmount(@PathVariable String email, @PathVariable String currencyFrom) {
		BankAccount userAccount = repo.findByEmail(email);
		List<AccountCurrencies> userCurrencies = userAccount.getCurrencies();
		for (AccountCurrencies accountCurrency : userCurrencies) {
			if (accountCurrency.getCurrency().equals(currencyFrom)) {
				return accountCurrency.getAmount();
			}
		}
		return null;
	}

	// localhost:8700/bank-account/account?email=test&from=EUR&to=RSD&quantity=20&totalAmount=5
	@PutMapping("/bank-account/account")
	public ResponseEntity<?> updateAccountCurrency(@RequestParam String email, @RequestParam String from,
			@RequestParam String to, @RequestParam double quantity, @RequestParam double totalAmount) {
		BankAccount existingAccount = repo.findByEmail(email);
		if (existingAccount == null) {
			String errorMessage = "The account with the passed id does not exist.";
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
		} else {
			List<AccountCurrencies> currenciesInAccount = existingAccount.getCurrencies();
			AccountCurrencies fromCurrency = null;
			AccountCurrencies toCurrency = null;

			for (AccountCurrencies currency : currenciesInAccount) {
				if (currency.getCurrency().equals(from)) {
					fromCurrency = currency;
				}
				if (currency.getCurrency().equals(to)) {
					toCurrency = currency;
				}

			}
			double fromCurrencyAmount = fromCurrency.getAmount();
			if(fromCurrencyAmount>0) {
			fromCurrency.setAmount(fromCurrencyAmount - quantity);
			if (toCurrency == null) {
				toCurrency = new AccountCurrencies();
				toCurrency.setCurrency(to);
				toCurrency.setAmount(totalAmount);
				toCurrency.setBankAccount(existingAccount);

				currenciesInAccount.add(toCurrency);
			} else {
				double toCurrencyAmount = toCurrency.getAmount();
				toCurrency.setAmount(toCurrencyAmount + totalAmount);
			}
			repoAcc.save(fromCurrency);
			repoAcc.save(toCurrency);
			BankAccount updatedBankAccount = repo.save(existingAccount);
			return ResponseEntity.ok(updatedBankAccount);
			}
			else {
				String errorMessage = "There is no enoguh amount for conversion.";
				return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
			}
		}
	}

}
