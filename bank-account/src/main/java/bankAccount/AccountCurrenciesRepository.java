package bankAccount;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCurrenciesRepository extends JpaRepository<AccountCurrencies, Long> {

}
