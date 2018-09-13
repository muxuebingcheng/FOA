package org.foa.businesslogic;

import com.google.gson.Gson;
import org.foa.data.optiondata.OptionDAO;
import org.foa.data.transactiondata.TransactionDAO;
import org.foa.data.userdata.UserDAO;
import org.foa.entity.*;
import org.foa.util.ResultMessage;
import org.foa.util.SortDTO;
import org.foa.util.SortUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.foa.entity.TransactionDirection.SELL;

@RestController
@RequestMapping("/TransactionBl")
public class TransactionBl {
    private Gson gson = new Gson();

    @Autowired
    private TransactionDAO transactionDAO;

    @Autowired
    private OptionDAO optionDAO;

    @Autowired
    private UserDAO userDAO;
    /*
     * 1. ResultMessage addTransaction(Transaction t);
     * 2. ResultMessage deleteTransaction(Transaction t);
     * 3. ResultMessage modifyTransaction(Transaction t);
     * 4. Transaction findTransactionById(long tid);
     * 5. List<Transaction> findAllTransactions(); //默认按照时间倒序排序
     * 6. List<Transaction> findTransactionsByTerm(List<SearchTerm> terms);
     * SearchTerm为接⼝，包括AscTime,DescTime,Period,Profit,Portfolio
     * public interface SearchTerm {
     * List<Transaction> filter(List transactions);
     * }
     * public class Transaction{
     * long tid;
     * LocalDateTime time;
     * List<Option> portfolio;
     * double profit;
     * }
     */

    /**
     * 购买单个期权
     * @param optionAbbr 期权合约简称
     * @param type OPEN 开仓 CLOSE 平仓
     * @param direction BUY 买进 SELL 卖出
     * @param num 交易数量
     * @param userId 用户id
     * @return resultMessage
     */
    @RequestMapping("/purchaseOption")
    @Transactional
    public ResultMessage purchaseOption(@RequestParam String optionAbbr, @RequestParam TransactionType type, @RequestParam TransactionDirection direction, @RequestParam Integer num, @RequestParam String userId){
        try {
            User user = userDAO.getOne(userId);
            double balance = user.getUserInfo().getBalance();
            Option option = optionDAO.findFirstByOptionAbbrOrderByTimeDesc(optionAbbr);

            if (balance < option.getLatestPrice() * num){
                return ResultMessage.FAILURE;
            }

            UserInfo userInfo = user.getUserInfo();
            userInfo.setBalance(balance - option.getLatestPrice() * num);
            user.setUserInfo(userInfo);
            userDAO.saveAndFlush(user);

            Transaction transaction = new Transaction();
            transaction.setTransactionType(type);
            transaction.setTransactionDirection(direction);
            transaction.setQuantity(num);
            transaction.setUserId(userId);
            transaction.setOptionAbbr(optionAbbr);
            transaction.setTime(LocalDateTime.now());
            transaction.setPrice(option.getLatestPrice());
            return transactionDAO.saveAndFlush(transaction).getTid() == 0 ? ResultMessage.FAILURE : ResultMessage.SUCCESS;
        }catch (DataAccessException|PersistenceException e){
            return ResultMessage.FAILURE;
        }
    }

    /**
     * addTransaction
     * @param transactionJson json of entity Transaction
     * @return resultMessage, SUCCESS or FAILURE
     */
    @RequestMapping("/addTransaction")
    @Transactional
    public ResultMessage addTransaction(@RequestParam String transactionJson){
        Transaction transaction = gson.fromJson(transactionJson, Transaction.class);
        try {
            transactionDAO.saveAndFlush(transaction);
        } catch (DataAccessException|PersistenceException e){
            return ResultMessage.FAILURE;
        }
        return ResultMessage.SUCCESS;
    }

    /**
     * delete
     * @param transactionJson json of entity transaction
     * @return resultMessage
     */
    @RequestMapping("/deleteTransaction")
    @Transactional
    public ResultMessage deleteTransaction(@RequestParam String transactionJson){
        Transaction transaction = gson.fromJson(transactionJson,Transaction.class);
        try {
            transactionDAO.delete(transaction);
        } catch (DataAccessException|PersistenceException e){
            return ResultMessage.FAILURE;
        }
        return ResultMessage.SUCCESS;
    }

    /**
     * delete in batch
     * @param tids list of transactions' ids
     * @return resultMessage
     */
    @RequestMapping("/deleteTransactionInBatch")
    @Transactional
    public ResultMessage deleteTransactionInBatch(@RequestParam long[] tids){
        try {
            for (long tid : tids) {
                transactionDAO.deleteById(tid);
            }
        } catch (DataAccessException|PersistenceException e){
            return ResultMessage.FAILURE;
        }
        return ResultMessage.SUCCESS;
    }

    /**
     * modify
     * @param transactionJson json of entity transaction
     * @return resultMessage
     */
    @RequestMapping("/modifyTransaction")
    @Transactional
    public ResultMessage modifyTransaction(@RequestParam String transactionJson){
        Transaction transaction = gson.fromJson(transactionJson,Transaction.class);
        try {
            transactionDAO.saveAndFlush(transaction);
        } catch (DataAccessException|PersistenceException e){
            return ResultMessage.FAILURE;
        }
        return ResultMessage.SUCCESS;
    }

    /**
     *
     * @param tid id of transaction
     * @return json of transaction
     */
    @RequestMapping("/findTransactionById")
    public Transaction findTransactionById(@RequestParam long tid){
        try {
            return transactionDAO.getOne(tid);
        } catch (DataAccessException|PersistenceException e){
            return null;
        }
    }

    /**
     *
     * @return all transactions
     */
    @RequestMapping("/findAllTransactions")
    public List<Transaction> findAllTransactions(){
        try {
            return transactionDAO.findAll();
        } catch (PersistenceException e){
            return new ArrayList<>();
        }
    }

    /**
     *
     * @param userId 用户名
     * @return 该用户的交易记录
     */
    @RequestMapping("/findTransactionByUser")
    public List<Transaction> findTransactionsByUser(@RequestParam String userId){
        try {
            List<Transaction> transactions = transactionDAO.findByUserIdOrderByTimeDesc(userId);
            return transactions;
        } catch (PersistenceException e){
            return new ArrayList<>();
        }
    }

    /**
     *
     * @param termsJson json of SortDTOs' list
     * @return transaction arranged according to the sortDTOs
     * org.foa.util.SortDTO
     */
    @RequestMapping("/findTransactionsByTerm")
    @SuppressWarnings("unchecked")
    public List<Transaction> findTransactionsByTerm(@RequestParam String termsJson){
        try {
            List<SortDTO> sortDTOS = gson.fromJson(termsJson, List.class);
            return transactionDAO.findAll(SortUtil.sortBy(sortDTOS.toArray(new SortDTO[0])));
        } catch (DataAccessException|PersistenceException e){
            return new ArrayList<>();
        }
    }

    /**
     * calculate the total income of specific user, but only the cash will be counted.
     * @param userId user id
     * @return the total income
     */
    @RequestMapping("/calcIncome")
    public Double calcIncome(@RequestParam String userId){
        List<Transaction> transactions = transactionDAO.findByUserIdOrderByTimeDesc(userId);
        return calcIncomeInPeriod(transactions);
    }

    /**
     * calculate the income of one user yesterday, only cash counted
     * @param userId user id
     * @return user's income yesterday
     */
    @RequestMapping("/calcIncomeYesterday")
    public Double calcIncomeYesterday(@RequestParam String userId){
        LocalDateTime now = LocalDateTime.now();
        now = now.minusDays(1);
        LocalDateTime startTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0);
        LocalDateTime endTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59);
        List<Transaction> transactions = transactionDAO.findByUserIdAndTimeAfterAndTimeBeforeOrderByTimeDesc(userId, startTime, endTime);
        return calcIncomeInPeriod(transactions);
    }

    /**
     * calc income according to transaction records
     * @param transactions transactions
     * @return income
     */
    private double calcIncomeInPeriod(List<Transaction> transactions){
        double income = 0;
        for (Transaction transaction : transactions) {
            double amount = transaction.getQuantity() * transaction.getPrice();
            if (transaction.getTransactionDirection() == SELL){
                income += amount;
            } else {//buy
                income -= amount;
            }
            income -= transaction.getFee();
        }
        return income;
    }
}
