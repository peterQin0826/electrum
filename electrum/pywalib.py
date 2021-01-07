#!/usr/bin/env python
# -*- coding: utf-8 -*-
import http
import math
import os
import json
import time
from enum import Enum
from os.path import expanduser
from electrum.util import Ticker, make_aiohttp_session
import requests
# from eth_accounts.account_utils import AccountUtils
from eth_keyfile import keyfile
from eth_utils import to_checksum_address
from web3 import HTTPProvider, Web3
from .eth_transaction import Eth_Transaction
from decimal import Decimal
import sqlite3
from .network import Network
from electrum.constants import read_json
eth_servers = {}

ETHERSCAN_API_KEY = "R796P9T31MEA24P8FNDZBCA88UHW8YCNVW"
INFURA_PROJECT_ID = "f001ce716b6e4a33a557f74df6fe8eff"
ROUND_DIGITS = 3
DEFAULT_GAS_PRICE_GWEI = 20
DEFAULT_GAS_LIMIT = 21000
GWEI_BASE = 1000000000
DEFAULT_GAS_SPEED = 1
KEYSTORE_DIR_PREFIX = expanduser("~")
# default pyethapp keystore path
KEYSTORE_DIR_SUFFIX = ".electrum/eth/keystore/"

# REQUESTS_HEADERS = {
#     "User-Agent": "https://github.com/AndreMiras/PyWallet",
# }

import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class InsufficientFundsException(Exception):
    """
    Raised when user want to send funds and have insufficient balance on address
    """
    pass


class InsufficientERC20FundsException(Exception):
    """
    Raised when user want to send ERC20 contract tokens and have insufficient balance
    of these tokens on wallet's address
    """
    pass


class ERC20NotExistsException(Exception):
    """
    Raised when user want manipulate with token which doesn't exist in wallet.
    """
    pass


class InvalidTransactionNonceException(Exception):
    """
    Raised when duplicated nonce occur or any other problem with nonce
    """
    pass


class InvalidValueException(Exception):
    """
    Raised when some of expected values is not correct.
    """
    pass

class InvalidAddress(ValueError):
    """
    The supplied address does not have a valid checksum, as defined in EIP-55
    """
    pass

class InvalidPasswordException(Exception):
    """
    Raised when invalid password was entered.
    """
    pass


class InfuraErrorException(Exception):
    """
    Raised when wallet cannot connect to infura node.
    """

class UnknownEtherscanException(Exception):
    pass


class NoTransactionFoundException(UnknownEtherscanException):
    pass


def get_abi_json():
    root_dir = os.path.dirname(os.path.abspath(__file__))
    abi_path = os.path.join(root_dir, '.', 'abi.json')
    with open(abi_path) as f:
        fitcoin = json.load(f)
    return fitcoin

def handle_etherscan_response_json(response_json):
    """Raises an exception on unexpected response json."""
    status = response_json["status"]
    message = response_json["message"]
    if status != "1":
        if message == "No transactions found":
            raise NoTransactionFoundException()
        else:
            raise UnknownEtherscanException(response_json)
    #assert message == "OK"


def handle_etherscan_response_status(status_code):
    """Raises an exception on unexpected response status."""
    if status_code != http.HTTPStatus.OK:
        raise UnknownEtherscanException(status_code)


def handle_etherscan_response(response):
    """Raises an exception on unexpected response."""
    handle_etherscan_response_status(response.status_code)
    handle_etherscan_response_json(response.json())


def requests_get(url):
    try:
        return requests.get(url, timeout=2, verify=False)
    except BaseException as e:
        raise e

headers = {
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36"
}

class PyWalib:
    web3 = None
    market_server = None
    tx_list_server = None
    gas_server = None
    symbols_price = {}
    config = None
    chain_type = None
    chain_id = None
    conn = None
    cursor = None
    def __init__(self, config, chain_type="mainnet", path=""):
        PyWalib.chain_type = chain_type
        PyWalib.config = config
        PyWalib.conn = sqlite3.connect(path)
        PyWalib.cursor = self.conn.cursor()
        self.create_db()
        self.init_symbols()

    def create_db(self):
        PyWalib.cursor.execute("CREATE TABLE IF NOT EXISTS txlist (tx_hash TEXT PRIMARY KEY, address TEXT, time INTEGER, tx TEXT)")

    def init_symbols(self):
        symbol_list = self.config.get("symbol_list", {'ETH':'','EOS':''})
        for symbol in symbol_list:
            PyWalib.symbols_price[symbol] = PyWalib.get_currency(symbol, 'BTC')
        global symbol_ticker
        symbol_ticker = Ticker(1*60, self.get_symbols_price)
        symbol_ticker.start()

    def get_symbols_price(self):
        try:
            for symbol, price in PyWalib.symbols_price.items():
                PyWalib.symbols_price[symbol] = self.get_currency(symbol, 'BTC')
                PyWalib.config.set_key("symbol_list", PyWalib.symbols_price)
                time.sleep(1)
        except BaseException as e:
            raise e

    @classmethod
    def get_json(cls, url):
        network = Network.get_instance()
        proxy = network.proxy if network else None
        with make_aiohttp_session(proxy) as session:
            with session.get(url) as response:
                response.raise_for_status()
                # set content_type to None to disable checking MIME type
                return response.json(content_type=None)

    @classmethod
    def get_currency(cls, from_cur, to_cur):
        try:
            out_price = {}
            for server in PyWalib.market_server:
                for name, url in server.items():
                    if name == "coinbase":
                        url += from_cur
                        response = requests.get(url, timeout=5, verify=False)
                        json = response.json()
                        return [str(Decimal(rate)) for (ccy, rate) in json["data"]["rates"].items() if ccy == to_cur][0]
                    # if name == "binance":
                    #     url += from_cur.upper()+to_cur.upper()
                    #     try:
                    #         response = requests.get(url, timeout=3, verify=False)
                    #         obj = response.json()
                    #         out_price[name] = obj['data']['lastprice']
                    #     except BaseException as e:
                    #         pass
                    elif name == 'bixin':
                        url += from_cur.upper() + '/' + to_cur.upper()
                        try:
                            response = requests.get(url, timeout=3, verify=False)
                            obj = response.json()
                            #out_price[name] = obj['data']['price']
                            return obj['data']['price']
                        except BaseException as e:
                            pass
                    # elif name == 'huobi':
                    #     url += from_cur.lower() + to_cur.lower()
                    #     try:
                    #         response = requests.get(url, timeout=3, verify=False)
                    #         obj = response.json()
                    #         out_price[name] = (obj['data']['bid'][0] + obj['data']['ask'][0])/2.0
                    #     except BaseException as e:
                    #         pass
                    # elif name == 'ok':
                    #     print("TODO")

            # return_price = 0.0
            # for price in out_price.values():
            #     return_price += float(price)
            # return return_price/len(out_price)
        except BaseException as e:
            print(f"get symbol price error {e}")
            pass

    @staticmethod
    def get_web3():
        return PyWalib.web3

    @staticmethod
    def set_server(info):
        PyWalib.market_server = info['Market']
        PyWalib.tx_list_server = info['TxliServer']
        PyWalib.gas_server = info['GasServer']
        for i in info['Provider']:
            if PyWalib.chain_type in i:
                url = i[PyWalib.chain_type]
                chain_id = i['chainid']
        PyWalib.web3 = Web3(HTTPProvider(url))
        PyWalib.chain_id = chain_id

    @staticmethod
    def get_coin_price(from_cur):
        try:
            from_cur = from_cur.upper()
            if from_cur in PyWalib.symbols_price:
                symbol_price = PyWalib.symbols_price[from_cur]
                return symbol_price if symbol_price is not None else PyWalib.get_currency(from_cur, 'BTC')
            else:
                symbol_price = PyWalib.get_currency(from_cur, 'BTC')
                PyWalib.symbols_price[from_cur] = symbol_price
                PyWalib.config.set_key("symbol_list", PyWalib.symbols_price)
                return symbol_price
        except BaseException as e:
            raise e

    def get_gas_price(self):
        try:
            #response = requests.get(eth_servers['GasServer'], headers=headers)
            if PyWalib.gas_server is not None:
                response = requests.get(PyWalib.gas_server, headers=headers)
                obj = response.json()
                out = dict()
                if obj['code'] == 200:
                    for type, wei in obj['data'].items():
                        fee_info = dict()
                        fee_info['price'] = int(self.web3.fromWei(wei, "gwei"))
                        if type == "rapid":
                            fee_info['time'] = "15 Seconds"
                        elif type == "fast":
                            fee_info['time'] = "1 Minute"
                        elif type == "standard":
                            fee_info['time'] = "3 Minutes"
                        elif type == "timestamp":
                            fee_info['time'] = "> 10 Minutes"
                        out[type] = fee_info
                return json.dumps(out)
        except BaseException as ex:
            raise ex

    def get_max_use_gas(self, gas_price):
        gas = gas_price * DEFAULT_GAS_LIMIT
        return self.web3.fromWei(gas * GWEI_BASE, 'ether')

    def get_transaction(self, from_address, to_address, value,
                        contract=None, gasprice=None, none=None):
        if (
            not self.web3.isAddress(from_address)
            or not self.web3.isAddress(to_address)
            or (contract and not self.web3.isAddress(contract.get_address()))
        ):
            raise InvalidAddress()

        try:
            if contract:
                decimal = contract.get_decimals()
                value = int(Decimal(value) * pow(10, decimal))
            else:
                value = self.web3.toWei(value, "ether")

            gasprice = DEFAULT_GAS_PRICE_GWEI if gasprice is None else self.web3.toWei(gasprice, "gwei")

            assert value > 0
            assert gasprice > 0
        except (ValueError, AssertionError, TypeError):
            raise InvalidValueException()

        nonce = (self.web3.eth.getTransactionCount(from_address)
                 if none is None else int(none))

        if contract is None:  # create ETH transaction dictionary
            tx_dict = Eth_Transaction.build_transaction(
                to_address=self.web3.toChecksumAddress(to_address),
                value=value,
                gas=DEFAULT_GAS_LIMIT,
                gas_price=gasprice,
                nonce=nonce,
                chain_id=int(PyWalib.chain_id)
            )
        else:  # create ERC20 contract transaction dictionary
            data_for_contract = Eth_Transaction.get_tx_erc20_data_field(to_address, value)

            # check whether there is sufficient ERC20 token balance
            _, erc20_balance = self.get_balance(from_address, contract)
            if value > erc20_balance:
                raise InsufficientERC20FundsException()

            # calculate how much gas I need, unused gas is returned to the wallet
            estimated_gas = self.web3.eth.estimateGas(
                {'to': contract.get_address(),
                 'from': from_address,
                 'data': data_for_contract
                 })
            estimated_gas = int(estimated_gas * 1.2)

            tx_dict = Eth_Transaction.build_transaction(
                to_address=self.web3.toChecksumAddress(contract.get_address()),
                value=0,
                gas=estimated_gas,
                gas_price=gasprice,
                nonce=nonce,
                chain_id=int(PyWalib.chain_id),
                data=data_for_contract
            )

        # check whether there is sufficient eth balance for this transaction
        balance = self.web3.eth.getBalance(from_address)
        fee_cost = int(tx_dict['gas'] * tx_dict['gasPrice'])
        eth_required = fee_cost + (value if not contract else 0)

        if eth_required > balance:
            raise InsufficientFundsException()

        return tx_dict

    def sign_and_send_tx(self, account, tx_dict):
        return Eth_Transaction.send_transaction(account, self.web3, tx_dict).hex()

    def serialize_and_send_tx(self, tx_dict, vrs):
        return Eth_Transaction.serialize_and_send_tx(self.web3, tx_dict, vrs).hex()

    @staticmethod
    def get_balance(wallet_address, contract=None):
        if contract is None:
            eth_balance = PyWalib.get_web3().fromWei(PyWalib.get_web3().eth.getBalance(wallet_address), 'ether')
            return "eth", eth_balance
        else:
            erc_balance = contract.get_balance(wallet_address)
            return contract.get_symbol(), erc_balance

    # def get_balance_web3(self, address):
    #     """
    #     The balance is returned in ETH rounded to the second decimal.
    #     """
    #     address = to_checksum_address(address)
    #     balance_wei = self.web3.eth.getBalance(address)
    #     balance_eth = balance_wei / float(pow(10, 18))
    #     balance_eth = round(balance_eth, ROUND_DIGITS)
    #     return balance_eth

    @staticmethod
    def get_transaction_history(address, recovery=False):
        tx_list = PyWalib.get_transaction_history_fun(address, recovery=recovery)
        if len(tx_list) == 0 and not recovery:
            PyWalib.cursor.execute("SELECT * FROM txlist WHERE address=? ORDER BY time DESC Limit 10", (address,))
            result = PyWalib.cursor.fetchall()
            for info in result:
                tx_list.append(info)
        return tx_list

    @classmethod
    def tx_list_ping(cls, recovery=False):
        try:
            speed_list = {}
            for server in PyWalib.tx_list_server:
                for key, value in server.items():
                    try:
                        if -1 == key.find(PyWalib.chain_type):
                            continue
                        else:
                            if recovery:
                                if -1 == key.find("trezor"):
                                    continue
                        speed_list[key] = value
                        return speed_list
                    except BaseException as e:
                        pass
            return None
        except BaseException as e:
            raise e

    def get_tx_from_etherscan(address, url):
        url += (
            '?module=account&action=txlist'
            '&sort=asc'
            f'&address={address}'
            f'&apikey={ETHERSCAN_API_KEY}'
        )
        try:
            response = requests_get(url)
            handle_etherscan_response(response)
            response_json = response.json()
        except BaseException as e:
            print(f"error....when get_eth_history.....{e}")
            pass
            return []
        transactions = response_json['result']
        out_tx_list = []
        for transaction in transactions:
            value_wei = int(transaction['value'])
            value_eth = value_wei / float(pow(10, 18))
            value_eth = round(value_eth, ROUND_DIGITS)
            from_address = to_checksum_address(transaction['from'])
            to_address = transaction['to']
            # on contract creation, "to" is replaced by the "contractAddress"
            if not to_address:
                to_address = transaction['contractAddress']
            to_address = to_checksum_address(to_address)
            sent = from_address == address
            received = not sent
            extra_dict = {
                'time': transaction['timeStamp'],
                'value_eth': value_eth,
                'sent': sent,
                'received': received,
                'from_address': from_address,
                'to_address': to_address,
            }
            time = int(transaction['timeStamp'])
            PyWalib.cursor.execute("INSERT OR IGNORE INTO txlist VALUES(?, ?,?,?)", (transaction['hash'], address, time, json.dumps(extra_dict)))
            out_tx_list.append(extra_dict)
        PyWalib.conn.commit()
        out_tx_list.sort(key=lambda x: x['time'])
        out_len = 10 if len(out_tx_list) >= 10 else len(out_tx_list)
        return out_tx_list[:out_len]

    def get_recovery_flag_from_trezor(address, url):
        try:
            url += f'/address/{address}'
            response = requests_get(url)
            #handle_etherscan_response(response)
            response_json = response.json()
            txs = response_json['txs']
            return response_json['txids'] if txs != 0 else []
        except BaseException as e:
            print(f"get_tx_flag ...errr{e}")
            pass
            return []

    def get_tx_from_trezor(address, url):
        url += f'/address/{address}'
        try:
            response = requests_get(url)
            handle_etherscan_response(response)
            response_json = response.json()
            txids = response_json['txids']
        except BaseException as e:
            print(f"errror .....get address from trezor....{e}")
            pass
            return []
        out_tx_list = []
        for txid in txids:
            url += f'/tx/{txid}'
            try:
                response = requests_get(url)
                handle_etherscan_response(response)
                response_json = response.json()
            except BaseException as e:
                print(f"errror .....get tx from trezor....{e}")
                continue
            value_wei = int(response_json['value'])
            value_eth = value_wei / float(pow(10, 18))
            value_eth = round(value_eth, ROUND_DIGITS)
            from_address = to_checksum_address(response_json['vin'][0]['addresses'])
            to_address = response_json['vout'][0]['addresses']
            # on contract creation, "to" is replaced by the "contractAddress"
            # if not to_address:
            #     to_address = transaction['contractAddress']
            to_address = to_checksum_address(to_address)
            sent = from_address == address
            received = not sent
            extra_dict = {
                    'time': response_json['blockTime'],
                    'value_eth': value_eth,
                    'sent': sent,
                    'received': received,
                    'from_address': from_address,
                    'to_address': to_address,
            }
            time = int(response_json['blockTime'])
            PyWalib.cursor.execute("INSERT OR IGNORE INTO txlist VALUES(?, ?,?,?)",
                                   (response_json['txid'], address, time, json.dumps(extra_dict)))
            out_tx_list.append(extra_dict)
            PyWalib.conn.commit()
            out_tx_list.sort(key=lambda x: x['time'])
            out_len = 10 if len(out_tx_list) >= 10 else len(out_tx_list)
            return out_tx_list[:out_len]

    def get_transaction_history_fun(address, recovery=False):
        """
        Retrieves the transaction history from server list
        """
        address = to_checksum_address(address)
        tx_list = []
        speed_list = PyWalib.tx_list_ping(recovery=recovery)
        for server_key, url in speed_list.items():
            if -1 != server_key.find("trezor"):
                if recovery:
                    print(f"get_transaction history from trezor to recovery....{address, url}")
                    tx_list = PyWalib.get_recovery_flag_from_trezor(address, url)
                else:
                    print(f"get_transaction history from trezor....{address, url}")
                    tx_list = PyWalib.get_tx_from_trezor(address, url)
            elif -1 != server_key.find("etherscan"):
                print(f"get_transaction history from etherscan....{address, url}")
                tx_list = PyWalib.get_tx_from_etherscan(address, url)
            if len(tx_list) != 0:
                return tx_list
        return tx_list
    # @staticmethod
    # def get_out_transaction_history(address):
    #     """
    #     Retrieves the outbound transaction history from Etherscan.
    #     """
    #     transactions = PyWalib.get_transaction_history(address, PyWalib.chain_id)
    #     out_transactions = []
    #     for transaction in transactions:
    #         if transaction['extra_dict']['sent']:
    #             out_transactions.append(transaction)
    #     return out_transactions

    # TODO: can be removed since the migration to web3
    @staticmethod
    def get_nonce(address):
        """
        Gets the nonce by counting the list of outbound transactions from
        Etherscan.
        """
        try:
            out_transactions = PyWalib.get_out_transaction_history(
                address, PyWalib.chain_id)
        except NoTransactionFoundException:
            out_transactions = []
        nonce = len(out_transactions)
        return nonce

    @staticmethod
    def handle_web3_exception(exception: ValueError):
        """
        Raises the appropriated typed exception on web3 ValueError exception.
        """
        error = exception.args[0]
        code = error.get("code")
        if code in [-32000, -32010]:
            raise InsufficientFundsException(error)
        else:
            raise UnknownEtherscanException(error)