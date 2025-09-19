# Pointed `/pt` コマンド

Paper/Spigot サーバー向けのポイント管理コマンド。


---

## コマンド一覧

### ポイント参照
#### 現在ポイント
```
/pt getnow <type> <key|playerName|uuid> <scope>
```

#### 累計ポイント
```
/pt gettotal <type> <key|playerName|uuid> <scope>
```

#### 現在/累計 両方
```
/pt get <type> <key|playerName|uuid> <scope>
```

---

### ポイント操作（管理者）
#### 加算
```
/pt add <type> <key|playerName|uuid> <scope> <amount>
```

#### 減算
```
/pt sub <type> <key|playerName|uuid> <scope> <amount>
```

#### 残高設定
```
/pt set <type> <key|playerName|uuid> <scope> <newNow>
```

---

### ランキング
#### 日別
```
/pt rank daily <type> <scope> [day] [limit]
```
- yyyy-MM-dd
- limit default：10  

#### 週次
```
/pt rank weekly <type> <scope> [start] [end] [limit]
```
- end yyyy-MM-dd
- start yyyy-MM-dd
- limit default：10  

#### 全期間
```
/pt rank global <type> <scope> [limit]
```
- limit default：10  

---

## 使用例
```text
/pt getnow PLAYER Steve battle
/pt add PLAYER Steve shop 100
/pt sub PLAYER Steve shop 50
/pt set PLAYER Steve shop 1000

/pt rank daily PLAYER battle
/pt rank weekly TEAM battle 2025-09-14 2025-09-20 15
/pt rank global SYSTEM event 50
```

---

## タブ補完
- 第1引数：`getnow`, `gettotal`, `get`, `add`, `sub`, `set`, `rank`
- 第2引数（参照・操作系）：`PLAYER`, `TEAM`, `SYSTEM`
- 第2引数（rank）：`daily`, `weekly`, `global`
