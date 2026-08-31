# 多局小游戏服使用指南（multi 分支）

[README](../../README.md) | [简体中文使用指南](./README-zh.md)

本文介绍 `multi` 分支的**多局并行小游戏服模式**：一台服务器同时运行多局方块竞速，玩家在大厅选择游戏加入，每局游戏在独立的 `arena_<编号>` 世界中进行，结束自动回收并立刻开放新局。适合公开服务器运营。

运行要求：Minecraft Java 版 26.2、Paper 26.2、Java 25。不支持 Spigot。

---

## 一、安装与首次启动

1. 将插件 JAR 放入 `plugins/` 目录，启动服务器。
2. 插件会自动完成：
   - 创建**大厅世界**（默认名为 `lobby`，见 `lobby.world`），并施加保护（和平、禁止破坏/放置/伤害/饥饿/爆炸/丢落物）；
   - 创建一个空的**等待局**（默认维持 1 个，见 `arena.idle-rooms`）；
   - 生成 `config.yml`、`lang.yml`、五个方块库和两个翻译 JSON。
3. 首局游戏开始时，才会创建该局的独立世界 `arena_1`（名字 = `arena.world-prefix` + 局号），随机地形种子，不写盘。

### 目录结构

```
<服务器根目录>/
├── lobby/                    大厅世界（自动创建）
├── arena_1/ arena_2/ ...     正在运行或等待中的局世界
├── world-backups/            已回收局世界的备份（按保留数自动清理）
└── plugins/BlockRacing/
    ├── config.yml            全部设置（见下文）
    ├── lang.yml              中文文案（en 为 languages/en_us/lang.yml）
    ├── arenas/
    │   └── 1/progress.yml    每局的崩溃恢复存档（默认关闭恢复时基本为空）
    └── ...
```

> 已回收的世界不会立刻删除：先整体搬进 `world-backups/arena_<id>_<时间戳>/`，再由后台线程慢慢删除。Windows 上个别文件被占用导致删不干净是正常现象，会在下次启动时继续清理，不影响开新局。

---

## 二、配置详解（config.yml）

| 键 | 默认 | 说明 |
|---|---|---|
| `medium-block` … `end-block` | false | 追加的目标方块库开关（准备菜单里也能改） |
| `block-amount` | 50 | 每队目标方块数量 |
| `game-mode` | normal | normal=两队不同目标且互塞箱子；racing=同目标竞速 |
| `speed-mode` | false | 极速模式（额外装备/效果，每方块 3 分） |
| `max-team-chest-num` | 8 | 每队共享箱子数量（≤53） |
| `max-team-waypoint-num` | 8 | 每队记录点数量（≤53） |
| `max-roll-count` | 3 | 每队每局轮换目标次数 |
| `recovery.enabled` | **false** | 服务器崩溃后是否恢复上一局（见"管理员指南"） |
| `lobby.world` | lobby | 大厅世界名，不存在时自动创建 |
| `arena.world-prefix` | arena_ | 局世界名前缀 |
| `arena.backup-keep` | 8 | `world-backups/` 中保留的已回收世界数量 |
| `arena.min-players` | 2 | 开局的最低人数 |
| `arena.max-players` | 8 | 单局人数上限（满员后自动开放新等待局） |
| `arena.auto-start` | true | 满足开局条件后是否自动开始 |
| `arena.idle-rooms` | 1 | 始终保持的空等待局数量 |
| `arena.max-game-minutes` | 240 | 看门狗：一局超过该时长强制结束 |
| `arena.countdown-seconds` | 10 | 全员准备后的开局倒计时秒数；**0 = 关闭倒计时立即开局** |

> 注意：`game-mode`、`speed-mode`、方块开关、方块数量这些"房间设置"在准备菜单中修改时仍会写回 config.yml（保留旧行为）。多局并行时，新等待局会以 config.yml 的当前值初始化。

---

## 三、玩家流程

1. **进服** → 直接落在大厅，聊天栏有语言选择提示（首次）。
2. **选局**：潜行 + 副手切换（默认 Shift+F）打开选择菜单，或使用 `/arena join [编号]`、`/arena join` 快速加入。菜单里可看到每局的状态和人数。
3. **准备**：进入等待局后再次 Shift+F 打开准备菜单——加入红/蓝队、调整目标方块设置、点击"准备"。
4. **自动开局**：人数达到 `min-players` 且**全员准备**后，广播 N 秒倒计时（`countdown-seconds`）；倒计时期间有人取消准备/离开会自动取消倒计时。想跳过等待可以直接点菜单里的"开始游戏"（手动立即开局）。
5. **游戏**：开局随机传送到本局世界（尽量避开海洋），收集当前目标方块（背包里或队伍箱子里都算），先清空目标列表的队伍获胜。
6. **结束**：展示获胜标题与收集排行榜，10 秒后所有人自动回到大厅（背包等状态还原），该局世界进入备份并被回收，新等待局立即开放。
7. **中途离开**：`/arena leave` 随时回大厅（状态还原）；掉线重连会回到原局（对局中途重连为旁观或按队伍重新初始化）。

---

## 四、命令一览

### 通用

| 命令 | 说明 |
|---|---|
| `/arena join [编号]` | 加入指定（或第一个可加入的）游戏 |
| `/arena leave` | 离开当前游戏，返回大厅 |
| `/arena list` | 列出所有局的状态与人数 |
| `/language [auto\|zh_cn\|en_us]` | 切换个人显示语言 |
| `/shout <消息>`（别名 `/all`，游戏中也可用 `@`/`!` 前缀） | 全服喊话 |

### 游戏内（需在局内）

| 命令 | 说明 |
|---|---|
| `/menu` | 打开当前局的菜单（等局=准备菜单，游戏中=游戏菜单） |
| `/menu chest [编号]` | 直接打开队伍箱子 |
| `/menu waypoints [use <编号>]` | 查看/使用记录点 |
| `/menu roll` | 发起目标轮换投票 |
| `/menu locate` | 购买 locate 指令权限 |
| `/menu randomTP` | 随机传送（首次免费，之后扣 2 分） |
| `/tp <队友>` | 传送到队友（旁观者可传给任何人） |
| `/waypoint remove <编号>` | 删除记录点 |
| `/block <队伍> <编号>` | 查询目标方块（管理权限） |
| `/locatebiome` `/locatestructure` | 购买后可用，一次性 |

### 管理员（权限 `blockracing.admin`，默认 OP）

| 命令 | 说明 |
|---|---|
| `/bradmin start` | 立即开始所在（或第一个）等待局 |
| `/bradmin stop` | 强制结束所在局并回收 |
| `/debug reload` | 重载文案与方块库 |
| `/debug skip <队> <编号\|all>`、`/debug setscore …`、`/debug setteam …` | 测试用 |

---

## 五、服务器管理员指南

### 权限

| 节点 | 默认 | 内容 |
|---|---|---|
| `blockracing.user` | true | `/arena`、`/language` 等玩家命令 |
| `blockracing.admin` | op | `blockracing.user` + `/bradmin`、`/block`、`/debug` |

公开服建议安装 LuckPerms，把 `blockracing.admin` 只授予管理组。

### 无人值守运营

- `arena.auto-start: true` + `countdown-seconds: 10` 下，服务器完全自动运转：空等待局常备（`idle-rooms`）→ 玩家加入准备 → 倒计时开局 → 结束回收 → 新局开放。
- 一局超过 `max-game-minutes` 会被看门狗强制结束回收，防止卡死的局占用世界。
- `idle-rooms` 调大可让多批玩家同时各自开局；`max-players` 限制单局规模。

### 世界与磁盘

- 每局世界结束时搬入 `world-backups/`，保留 `backup-keep` 份，超出自动清理（开服时也会清理）。
- 磁盘紧张就调小 `backup-keep`；`world-backups/` 里的内容可以随时手动删除，不影响运行。
- 不要手动编辑或删除正在使用的 `arena_*` 世界；服务器关闭后残留的 `arena_*` 目录会在下次启动自动搬入备份。

### 崩溃恢复（默认关闭）

- `recovery.enabled: true` 时：若服务器在游戏进行中崩溃（局世界目录还在），下次启动会把该局完整恢复（分数、方块进度、队伍、箱子、记录点、世界本身）。
- 正常结束的局（含 `/bradmin stop`）会同时清掉存档与世界，**不会**被恢复；存档属于哪个局号也会被校验。
- 旧版本（26.2.x 好友服）的 `game-progress.yml` 会在开启恢复后的首次启动自动迁移到新路径，无需手动处理。

### 从旧版本升级

1. 备份 `plugins/BlockRacing`。
2. 旧 `config.yml`/`lang.yml` 缺少 `recovery`、`arena`、`lobby` 等新段，缺失的键自动取默认值；建议对照 `src/main/resources` 里的默认文件手动合并自定义内容。
3. `/restartgame` 投票重置已**移除**：对玩家而言对局结束即自动回收；管理员用 `/bradmin stop`；整服重置不再需要（每局世界独立回收）。

---

## 六、常见问题

**Q：开局为什么要求全员准备？**
达到 `min-players` 且所有人点过"准备"后开始 10 秒倒计时；想尽快开局，全员准备即可，或由管理员 `/bradmin start` 跳过等待。倒计时的长短和开关见 `countdown-seconds`。

**Q：我可以同时在多个局里吗？**
不能。加入新局前必须 `/arena leave`，否则提示"你已经在一个游戏中"。

**Q：不同局的玩家会互相干扰吗？**
不会。聊天（队伍聊天按局隔离）、计分板、广播、音效、团队箱子、记录点全部按局隔离；每局世界独立。全服可见的只有 `/shout` 和大厅聊天。

**Q：大厅里能 PVP / 建造吗？**
大厅受保护：无法破坏/放置、无伤害、无饥饿、无掉落物。游戏在世界 `arena_*` 内正常进行。

**Q：玩家中途进服怎么处理？**
进服落在大厅。若该玩家掉线前在某局中，重连会自动回到该局（对局进行中则继续，未开始则回准备阶段）。

**Q：`/restartgame` 去哪了？**
已删除。对局结束自动回收世界并开放新局，无需手动重置；管理员可用 `/bradmin stop` 强制结束当前局。
