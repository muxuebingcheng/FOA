package org.foa.businesslogic;

import org.foa.FoaApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.transaction.Transactional;

/**
 * @author miaomuzhi
 * @since 2018/10/28
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FoaApp.class)
@WebAppConfiguration
public class AutoPurchaseTest {

    @Autowired
    private AutoPurchase autoPurchase;

    @Test
    @Transactional(dontRollbackOn = Exception.class)
    @Rollback(false)//should it be true
    public void generateTransactionsRecord() {
        try {
            autoPurchase.generateTransactionsRecord();
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}