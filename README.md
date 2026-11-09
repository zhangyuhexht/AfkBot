# AfkBot · 挂机假人插件

面向 **Paper 26.3（Minecraft 26.3）** 的轻量假人插件：管理员可以召唤"挂机假人"，
假人像真实玩家一样按服务器视距加载地形，但**不占用人数上限**，也**绝不会被推动**。

> 适合需要"挂机保活 / 保持区块加载 / 让服务器看起来有在线玩家"的场景。

---

## 功能特性

- **仅管理员可用**：权限节点 `afkbot.admin`（默认 OP）
- **完整命令集**：创建 / 移除 / 列表 / 传送 / 批量移除 / 重载
- **一键传送**：`/bot list` 中每条假人自带可点击的 `[传送到此]`、`[带过来]` 按钮
- **不占人数上限**：假人不会加入玩家列表，`max-players` 名额只留给真实玩家
- **自动加载地形**：与真实玩家完全一致的区块加载（遵循服务器的 view-distance / simulation-distance）
- **不能被推动**：关闭物理碰撞 + 关闭重力 + 周期性位置锚定，三重保险
- **可选 Tab 显示**：让假人看起来像真实在线玩家（可配置开关，`/bot reload` 实时生效）
- **可选无敌**：假人不会受到任何伤害
- **全中文**：命令提示、配置文件注释全部中文
- **热重载**：`/bot reload` 即时生效

---

## 环境要求

| 项目 | 版本 |
|------|------|
| 服务端 | Paper 26.3（Minecraft 26.3） |
| Java | **JDK 25**（构建与运行均需要） |

> Minecraft 26.3 起服务端不再混淆，本插件直接使用 Mojang 映射编译，无需重混淆即可运行。

---

## 安装

1. 从 Releases 页面下载 `AfkBot-1.0.0.jar`
2. 放入服务端 `plugins/` 目录
3. 启动服务端，插件会自动生成：
   - `plugins/AfkBot/config.yml` —— 功能配置（中文注释）
   - `plugins/AfkBot/messages.yml` —— 所有提示文本（可自由修改）
4. 修改配置后执行 `/bot reload`

---

## 命令

所有命令都需要 `afkbot.admin` 权限（默认 OP）。控制台同样可用。

| 命令 | 说明 |
|------|------|
| `/bot create <名称>` | 在你的位置创建假人 |
| `/bot create <名称> <x> <y> <z>` | 在指定坐标创建假人（控制台常用） |
| `/bot remove <名称>` | 移除指定假人 |
| `/bot list` | 查看所有假人，可点击传送 |
| `/bot tp <名称>` | 传送到假人身边 |
| `/bot tphere <名称>` | 把假人带到你身边（需同一世界） |
| `/bot removeall` | 移除全部假人 |
| `/bot reload` | 重载 `config.yml` 与 `messages.yml` |

**名称规则**：3–16 位字母、数字或下划线，且不能与在线玩家或已有假人重名。

### 使用示例

```
/bot create Shop_Guard 100 64 200     # 在固定坐标创建一个"商店守卫"
/bot list                             # 查看全部假人，点击 [传送到此] 直接飞过去
/bot tphere Shop_Guard                # 把假人带到你脚下
/bot removeall                        # 一键清空
```

---

## 配置（config.yml）

```yaml
# 假人数量上限
max-bots: 10

# 是否在 Tab 玩家列表中显示假人
show-in-tab: true

# 假人是否无敌
invulnerable: true
```

所有玩家可见的文本（含前缀、颜色）都在 `messages.yml` 中配置，支持 `&` 颜色代码。
`/bot reload` 会同时重载两个文件；修改 `show-in-tab` 后，现有假人的 Tab 显示会立即同步。

---

## 从源码构建

```bash
# 需要 JDK 25
git clone https://github.com/zhangyuhexht/AfkBot.git
cd AfkBot
gradle build
# 产物：build/libs/AfkBot-1.0.0.jar
```

项目使用 Paper 官方的 **paperweight-userdev** 构建工具访问服务端内部代码（NMS），
首次构建会从 Paper Maven 仓库自动下载开发套件，需要联网。

---

## 实现原理

这部分写给想改代码 / 想搞清"为什么假人不占人数还能加载地形"的人。

1. **假人是真实的 `ServerPlayer` 实体**
   通过 NMS 构造 `ServerPlayer`，配上假网络连接（`FakeConnection`，所有出站数据包直接丢弃），
   因此假人在世界里有实体、能 tick、能被其他玩家看到。

2. **不占人数上限**
   假人**不注册进 `PlayerList`**，所以：
   - `Bukkit.getOnlinePlayers()` 不包含它
   - 原版 `/list`、服务器 ping 的人数不统计它
   - 真实玩家永远不会因为假人而"服务器已满"

3. **加载地形（关键点）**
   Paper 26.x 使用 Moonrise 重写的区块系统，其中 `PlayerChunkLoader.addPlayer()` 会检查
   `ServerPlayer#isRealPlayer`，为 `false` 时直接跳过——这是普通插件创建假人后"不加载地形"的根本原因。
   本插件将假人标记为 `isRealPlayer = true` 后再调用 `ServerLevel#addNewPlayer()`，
   服务端即按真实玩家的方式注册区块加载（视距、模拟距离完全遵循服务器配置）。

4. **不能被推动**
   - `noPhysics = true`：玩家挤不动它（也避免了被挤压产生的位移）
   - `noGravity = true`：不会因为关闭碰撞而穿地掉落
   - 每 5 tick 归零速度并把位置锁回锚点：爆炸、钓鱼竿、活塞等极端情况也会被拉回原处

5. **Tab 列表**
   通过向在线玩家发送 `ClientboundPlayerInfoUpdatePacket` 实现，
   完全不依赖 `PlayerList`；玩家进服时自动补发，移除假人时发送对应的移除包。

---

## 已知限制

- 假人不在玩家列表中，因此不会出现在 `Bukkit.getOnlinePlayers()` / 原版 `/list` 里（这正是"不占人数"的代价）
- `/bot tphere` 仅支持同一世界：跨世界移动会破坏假人的维度状态，插件会提示并拒绝
- 假人名称允许与"从未上线过的名字"重名；若之后真有玩家用同名进服，Tab 中会出现两个同名条目
- 假人是**静止**的挂机假人，不会自动走动、不会自动钓鱼、不响应交易

---

## 更新日志

### v1.0.0
- 首个版本：创建 / 移除 / 列表 / 传送 / 批量移除 / 热重载
- 假人不占人数上限、按服务器视距加载地形、防推动、可选 Tab 显示与无敌
- 全中文提示与配置注释

---

## 反馈

遇到问题请提交 Issue，并附上：
- 服务端版本（`/version` 输出）
- 报错日志
- 复现步骤
