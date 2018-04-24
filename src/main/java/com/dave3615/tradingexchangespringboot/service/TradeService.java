package com.dave3615.tradingexchangespringboot.service;

import com.dave3615.tradingexchangespringboot.controller.TradeController;
import com.dave3615.tradingexchangespringboot.dao.TradeBuyDAO;
import com.dave3615.tradingexchangespringboot.dao.TradeSellDAO;
import com.dave3615.tradingexchangespringboot.dao.UserDAO;
import com.dave3615.tradingexchangespringboot.model.BuyOrder;
import com.dave3615.tradingexchangespringboot.model.SellOrder;
import com.dave3615.tradingexchangespringboot.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;


@Service
public class TradeService {

    @Autowired
    private TradeBuyDAO tradeBuyDAO;

    @Autowired
    private TradeSellDAO tradeSellDAO;

    @Autowired
    private UserDAO userDAO;

    private static final Logger logger = LogManager.getLogger(TradeController.class);

    public void placeBuyOrder(BuyOrder buyOrder) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userDAO.findByUsername(((UserDetails)principal).getUsername());
        buyOrder.setUser(user);
        buyOrder.setTime(new java.util.Date());
        buyOrder.setTotalAmount(buyOrder.getAmountLeft());
        tradeBuyDAO.save(buyOrder);
        bookOrders();
    }
    public void placeSellOrder(SellOrder sellOrder) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userDAO.findByUsername(((UserDetails)principal).getUsername());
        sellOrder.setUser(user);
        sellOrder.setTime(new java.util.Date());
        sellOrder.setTotalAmount(sellOrder.getAmountLeft());
        tradeSellDAO.save(sellOrder);
        bookOrders();
    }

    public void bookOrders(){
        List<BuyOrder> buyOrders = getBuyOrders();
        List<SellOrder> sellOrders = getSellOrders();
        outerloop:
        for (BuyOrder buyOrder: buyOrders) {
            for (SellOrder sellOrder : sellOrders) {
                if(buyOrder.getPrice() >= sellOrder.getPrice() && buyOrder.getAmountLeft() > 0 ){
                    completeOrder(buyOrder,sellOrder);
                }else{
                    break outerloop;
                }
            }
        }

    }

    public void completeOrder(BuyOrder buyOrder, SellOrder sellOrder){
        if(buyOrder.getPrice() >= sellOrder.getPrice()){
            logger.info("Trade started between buyer:" + buyOrder.getUser().getUsername() + "and seller: " + sellOrder.getUser().getUsername() + ".");
            long numberToBeTraded = (buyOrder.getAmountLeft() >= sellOrder.getAmountLeft()) ? sellOrder.getAmountLeft() : buyOrder.getAmountLeft();
            System.out.println(numberToBeTraded);
            if (buyOrder.getUser().getUsd() >= numberToBeTraded * sellOrder.getPrice() && sellOrder.getUser().getBitcoins() >= numberToBeTraded){
                buyOrder.getUser().setUsd(buyOrder.getUser().getUsd() - numberToBeTraded * sellOrder.getPrice());
                buyOrder.getUser().setBitcoins(buyOrder.getUser().getBitcoins() + numberToBeTraded);
                sellOrder.getUser().setUsd(sellOrder.getUser().getUsd() + numberToBeTraded * sellOrder.getPrice());
                sellOrder.getUser().setBitcoins(sellOrder.getUser().getBitcoins() - numberToBeTraded);
                buyOrder.setAmountLeft(buyOrder.getAmountLeft()-numberToBeTraded);
                sellOrder.setAmountLeft(sellOrder.getAmountLeft()-numberToBeTraded);
                tradeBuyDAO.save(buyOrder);
                tradeSellDAO.save(sellOrder);
                logger.info("Trade of " + numberToBeTraded + "Bitcoins was transfered for a price of " + sellOrder.getPrice() + "USD.");
            }else{
                logger.info("Transaction aborted. One user had insufficient balance");
            }
        }
    }

    public List<BuyOrder> getBuyOrders(){
        return tradeBuyDAO.findAllByOrderByPriceDesc();
    }
    public List<SellOrder> getSellOrders(){
        return tradeSellDAO.findAllByOrderByPriceDesc();
    }




}