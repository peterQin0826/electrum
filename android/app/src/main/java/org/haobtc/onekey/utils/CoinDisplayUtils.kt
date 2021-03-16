package org.haobtc.onekey.utils

import org.haobtc.onekey.bean.Assets
import org.haobtc.onekey.bean.CoinAssets
import org.haobtc.onekey.bean.ERC20Assets
import org.haobtc.onekey.constant.Vm
import java.math.BigDecimal
import java.math.RoundingMode

/**
 *
 * @Description:     统一管理 币种展示
 * @Author:         peter Qin
 *
 */
class CoinDisplayUtils {

 companion object {
  @JvmStatic
  fun getCoinBalanceDisplay(asset: Assets): String {
   when (asset) {
     is CoinAssets -> {
       when {
         asset.coinType.chainType.equals(Vm.CoinType.ETH.chainType, true) -> {
           return asset.balance.balance.setScale(6, RoundingMode.DOWN).stripTrailingZeros()
             .toPlainString()
         }
         asset.coinType.chainType.equals(Vm.CoinType.BTC.chainType, true) -> {
           return asset.balance.balance.setScale(8, RoundingMode.DOWN).stripTrailingZeros()
             .toPlainString()
         }
       }
     }
     is ERC20Assets -> {
       return asset.balance.balance.setScale(4, RoundingMode.DOWN).stripTrailingZeros()
         .toPlainString()
     }
   }
    return ""
  }

   @JvmStatic
   fun getCoinFeeDisplay(fee: String, coinType: Vm.CoinType): String {
     if (coinType.chainType.equals(Vm.CoinType.ETH.chainType)) {
       return BigDecimal(fee).setScale(6, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
     } else if (coinType.chainType.equals(Vm.CoinType.BTC.chainType)) {
       return BigDecimal(fee).setScale(8, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
     }
     return ""
   }

 }
}
