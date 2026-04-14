# BuildingBoard

Minecraft サーバー向けの建築依頼管理プラグインです。  
発注者が建築依頼を作成し、施工者が応募または選定され、範囲保護と報酬支払いを含めて管理できます。

## 主な機能

- GUI で依頼一覧を表示
- 発注者と施工者を分けた依頼管理
- 施工者は複数人まで確定可能
- 応募、承認、却下、辞退、募集期限後の手動追加
- WorldEdit の選択範囲を依頼範囲として利用
- 範囲内は発注者と確定施工者のみ編集可能
- 募集期限、作業期限、強制完了期限の管理
- 通知の永続化
- Vault を使った報酬支払いと返金
- 依頼作成時の報酬即時引き落とし

## 前提プラグイン

- Spigot/Paper 1.15 系
- WorldEdit
- Vault
- Vault 対応の経済プラグイン

## 現在の状態

実装済み:

- MySQL 永続化
- テーブル自動作成
- GUI 一覧、詳細、応募確認、応募者管理、通知表示
- 依頼作成 GUI / 編集 GUI
- 説明文の行単位編集
- 範囲保護
- 期限チェックの定期実行

未実装または簡易実装:

- 専用の依頼作成ウィザード風 UI
- 通知から依頼へ直接飛ぶ導線
- 高度なプレイヤー検索 UI
- 詳細な監査ログ画面

## インストール

1. Jar を `plugins/` に配置します
2. サーバーを起動します
3. 生成された `plugins/BuildingBoard/config.yml` を編集します
4. MySQL 接続情報を設定します
5. サーバーを再起動します

## 設定

`config.yml`

```yml
database:
  host: "127.0.0.1"
  port: 3306
  name: "buildingboard"
  username: "root"
  password: "password"
  use-ssl: false
  connection-parameters: "?characterEncoding=utf8&useUnicode=true&serverTimezone=UTC"

defaults:
  recruitment-deadline-days: 7
  work-deadline-days: 30
  force-complete-extra-days: 60

tasks:
  deadline-check-interval-seconds: 300
```

## 権限

- `buildingboard.command`
  - 基本コマンドの使用
- `buildingboard.admin`
  - 保護範囲のバイパス
  - 実装上、管理者として扱われる操作の実行

## コマンド

基本:

- `/bb gui`
- `/bb gui create`
- `/bb gui jobs`
- `/bb gui my`
- `/bb gui current`
- `/bb gui notifications`
- `/bb gui edit <jobId>`

依頼操作:

- `/bb create <reward> <title...>`
- `/bb edit <jobId> <recruitDays> <workDays> <title...>`
- `/bb info <jobId>`
- `/bb apply <jobId>`
- `/bb approve <jobId> <player>`
- `/bb decline <jobId> <player>`
- `/bb add <jobId> <player>`
- `/bb remove <jobId> <player>`
- `/bb withdraw <jobId>`
- `/bb complete <jobId>`
- `/bb cancel <jobId>`

説明文編集:

- `/bb lines list`
- `/bb lines add <message>`
- `/bb lines set <number> <message>`
- `/bb lines remove <number>`
- `/bb lines clear`

通知 / 管理:

- `/bb notifications`
- `/bb notifications all`
- `/bb readall`
- `/bb refunds`
- `/bb checkdeadlines`

## GUI

メインメニュー:

- 依頼を作成
- 募集中の依頼
- 自分の依頼
- 受注中の依頼
- すべての依頼
- 通知

依頼詳細:

- 状態
- 報酬総額
- 説明文
- 範囲
- 報酬分配
- 応募 / 辞退 / 完了 / 取消 / 編集

応募者一覧:

- 応募者の承認
- 応募者の却下
- 確定施工者の解除
- 募集期限後の施工者追加

## 依頼作成と報酬

依頼作成時に、指定した報酬総額は Vault 経由で即時引き落とされます。  
これにより、完了時の不払いを防ぎます。

依頼編集で報酬額を変更した場合:

- 増額: 差額を即時引き落とし
- 減額: 差額を即時返金
- 返金失敗時: 返金待ちとして記録

## 状態遷移

- `OPEN`
  - 募集中
- `IN_PROGRESS`
  - 募集期限後かつ確定施工者あり
- `WORK_DEADLINE_PASSED`
  - 作業期限超過
- `COMPLETED`
  - 完了
- `CANCELLED`
  - 取消
- `EXPIRED`
  - 失効

募集期限前に施工者を確定しても、状態は `OPEN` のままです。  
ただし確定施工者と発注者は、この段階でも保護範囲を編集できます。

## 範囲保護

保護対象:

- ブロック設置
- ブロック破壊
- バケツ操作
- ドアやボタンなどのインタラクト
- チェスト、作業台、かまどなどの利用
- ピストン
- 水 / 溶岩の流動

ルール:

- 発注者と確定施工者のみ編集可能
- `buildingboard.admin` は保護をバイパス可能
- 範囲内 -> 範囲外 の液体流出は禁止
- 範囲外 -> 範囲内 の液体流入は許可

## 説明文編集

依頼の説明は複数行で管理されます。  
GUI では概要だけ表示し、編集は `lines` コマンドで行います。

例:

```text
/bb lines add 外壁を石レンガで統一してください
/bb lines add 屋根はダークオークでお願いします
/bb lines set 2 屋根はブラックストーンでお願いします
/bb lines remove 1
```

## 返金

返金対象:

- 確定施工者 0 人のまま失効
- 依頼取消
- 報酬分配の端数
- 報酬減額時の差額返金

返金は DB に記録され、プレイヤーログイン時に再試行されます。

## 開発

ビルド:

```bash
./gradlew build
```

成果物:

- `build/libs/BuildingBoard-<version>.jar`

## 注意

- MySQL が利用できないと起動できません
- WorldEdit がないと起動できません
- Vault または経済プラグインがない場合、報酬処理を伴う依頼操作は失敗します
