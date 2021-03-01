package org.haobtc.onekey.bean;

import com.google.gson.annotations.SerializedName;

/**
 * 币种金额
 *
 * @author Onekey@QuincySx
 * @create 2021-01-19 7:15 PM
 */
public class BalanceCoinInfo {
    @SerializedName("balance")
    private String balance = "0.00";

    @SerializedName("coin")
    private String coin = "btc";

    @SerializedName("fiat")
    private String fiat = "0.00 USD";

    @SerializedName("wallet_name")
    private String walletName = "";

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getFiat() {
        return fiat;
    }

    public void setFiat(String fiat) {
        this.fiat = fiat;
    }

    public String address = "0x9a42D6dCD2519E94D037439c447a7a752Ba9F66c";

    public String icon = "https://assets.coingecko.com/coins/images/2518/large/weth.png?1547036627";
}
