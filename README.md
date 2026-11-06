# ChatCard

A chat plugin where you set everything that shows in chat, with a stats card when you hover a player's name.

Hover any player's name in chat to see their money, shards, kills, playtime and deaths. Click it to run a command. The
card, the chat format for every rank and the join, quit and death messages all come from the config.

## Features

- Chat format per rank, picked by permission
- Hover card on player names in chat, join and quit messages, with PlaceholderAPI placeholders
- Clicking a player's name runs a command from the config
- Custom join, first-join, quit and death messages (death messages keep the vanilla text in each player's language)
- Mentions: a player's name or `@name` is highlighted for them, with a sound and an actionbar
- Chat colors by permission: color codes, hex colors, gradients and formatting
- Clickable links for players with permission; links, domains and IPs are blocked for everyone else
- Chat filter: cooldown, repeated messages, too many capital letters and a word filter
- Emoji shortcuts like `:heart:` and `:skull:`
- Staff tools: mute chat, clear chat, slow mode and a delete button on every message
- `/togglechat` and `/togglementions` for players, saved across restarts
- Turns off the vanilla `/me` and `/teammsg` commands, so nobody can talk around the mute or the filter
- `&` colors, `&#RRGGBB` hex colors and MiniMessage tags (`<gradient>`, `<rainbow>`, `<bold>` ...) in every text
- Folia support
- `/chat reload` to reload the config without a restart

## Requirements

- Paper 1.21.7 or newer, or Folia (tested on Paper 1.21.7, Paper 26.1.2 and Canvas 26.3, a Folia fork)
- Java 25 or newer on 26.x servers, Java 21 or newer on 1.21.x servers
- [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) for placeholders (optional)

The default config uses these PlaceholderAPI expansions:

```
/papi ecloud download Player
/papi ecloud download Vault
/papi ecloud download Statistic
/papi ecloud download LuckPerms
```

Then run `/papi reload`.

`%shards%` in the default hover card is left for your shards plugin. Replace it with that plugin's placeholder.

## Installation

1. Put `ChatCard-<version>.jar` in your `plugins` folder.
2. Start the server. The plugin downloads Kotlin on first start.
3. Edit `plugins/ChatCard/config.yml` and `plugins/ChatCard/messages.yml`, then run `/chat reload`.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/chat mute` | `chatcard.mute` | Mutes or unmutes the chat |
| `/chat clear` | `chatcard.clear` | Clears the chat |
| `/chat slow <seconds>` | `chatcard.slow` | Players can send one message every X seconds. `0` turns it off |
| `/chat reload` | `chatcard.reload` | Reloads `config.yml` and `messages.yml` |
| `/togglechat` | `chatcard.togglechat` (everyone) | Hides other players' chat messages for you |
| `/togglementions` | `chatcard.togglementions` (everyone) | Turns mentions on or off for you |

`/chat` is short for `/chatcard`. Mute and slow mode reset when the server restarts.

The vanilla commands `/me` and `/teammsg` (`/tm`) send chat that skips the mute, slow mode and filters, so ChatCard
turns them off for players and hides them from tab completion.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `chatcard.staff` | op | Every staff command and bypass below |
| `chatcard.delete` | op | See the delete button before chat messages |
| `chatcard.bypass.mute` | op | Chat while the chat is muted |
| `chatcard.bypass.cooldown` | op | No cooldown and no slow mode |
| `chatcard.bypass.filter` | op | Skip the repeated message, caps and word filter |
| `chatcard.bypass.clear` | op | Keep your chat when it is cleared |
| `chatcard.links` | op | Send clickable links |
| `chatcard.chat.color` | op | `&a` color codes and `<red>` color tags |
| `chatcard.chat.hex` | op | `&#RRGGBB` and `<#RRGGBB>` hex colors |
| `chatcard.chat.gradient` | op | `<gradient:#FF0000:#0000FF>` and `<rainbow>` |
| `chatcard.chat.format` | op | `&l &o &n &m &k` and `<bold>`, `<italic>` ... |
| `chatcard.emojis` | everyone | Emoji shortcuts |
| `chatcard.format.<name>` | nobody | The permission you set on a chat format in `config.yml` |

Chat format permissions are not given to ops automatically. Give them with your permission plugin.

## Configuration

```yaml
chat:
  formats:
    vip:
      permission: chatcard.format.vip
      format: "%vault_prefix%{player}&7: &#FFE400{message}"
    default:
      permission: ""
      format: "%vault_prefix%{player}&7: &f{message}"
  delete-button: "&c✖ "
  delete-hover: "&cDelete this message"

hover:
  lines:
    - "%luckperms_prefix%%player_name%"
    - "&7------------"
    - "&#00FF11$ &fMoney: &#00FF11%vault_eco_balance_formatted%"
    - "&#A52CF5⭐ &fShards: &#A52CF5%shards%"
    - "&#FF0000🗡 &fKills: &#FF0000%statistic_player_kills%"
    - "&#FFE400⌚ &fPlaytime: &#FFE400%statistic_time_played%"
    - "&#FA7600☠ &fDeaths: &#FA7600%statistic_deaths%"
    - "&7------------"
  click: "/msg %player_name%"

join:
  message: "&8[&a+&8] &7{player}"
  first-join: "<gradient:#FFE400:#FA7600>Welcome</gradient> &f{player} &7to the server!"

quit:
  message: "&8[&c-&8] &7{player}"

death:
  message: "&#FA7600☠ &7{message}"

mentions:
  highlight: "&#FFE400@{name}"
  sound: "entity.experience_orb.pickup"
  volume: 1.0
  pitch: 1.0
  actionbar: "&#FFE400{player} &7mentioned you"

filter:
  cooldown: 1.0
  duplicate-seconds: 30
  caps:
    min-letters: 6
    max-percent: 60
  words:
    - fuck
    - shit
    - ...

emojis:
  ":heart:": "❤"
  ":star:": "★"
  ":skull:": "☠"
  ...

clear-lines: 100
```

| Key | Description |
|---|---|
| `chat.formats` | Checked from top to bottom. A player gets the first format whose `permission` they have. An empty permission matches everyone, so keep `default` last |
| `{player}` | The player's name with the hover card and the click command. Works in chat formats, join, quit and death messages |
| `{message}` | The chat message, or the vanilla death message in `death.message` |
| `chat.delete-button` | Shown before every chat message to players with `chatcard.delete` |
| `hover.lines` | The hover card. Placeholders are filled in for the player whose name it is |
| `hover.click` | The command the clicking player runs. Placeholders are filled in for the player whose name was clicked |
| `join.first-join` | Shown instead of `join.message` the first time a player joins |
| `mentions.highlight` | How a mention looks to the mentioned player. `{name}` is their name |
| `mentions.sound` | Any Minecraft sound, written as `entity.experience_orb.pickup` or `ENTITY_EXPERIENCE_ORB_PICKUP` |
| `mentions.actionbar` | `{player}` is the player who sent the message |
| `filter.cooldown` | Seconds between two messages of the same player |
| `filter.duplicate-seconds` | The same message again within this many seconds is blocked |
| `filter.caps` | A message with at least `min-letters` letters and more than `max-percent` capital letters is made lowercase |
| `filter.words` | Replaced with `*`. Matches whole words, any case, with repeated letters (`shiiit`) |
| `emojis` | Shortcut and the text it becomes |
| `clear-lines` | How many empty lines `/chat clear` sends |

Set a text to `""` to turn it off. That works for join, quit, first-join, death, the delete button, the click
command, the mention highlight, sound and actionbar, and every line in `messages.yml`. `hover.lines: []` turns the
hover card off.

Players' own messages never go through PlaceholderAPI, and only get colors with the permissions above. Tags like
`<click>` or `<hover>` always stay plain text.

### Hover card size

Minecraft has no character limit on the hover card. The limit is the player's screen:

- Lines wrap at half the screen width. On a 1080p screen at the largest GUI scale, that is about 40 characters.
- A card taller than the screen is cut off. On a 1080p screen at the largest GUI scale, that is about 25 lines.

Keep the card under about 20 lines and it fits on every screen.

### Message deletion

The delete button uses Minecraft's own message deletion, which only works on signed chat (`online-mode=true` with
`enforce-secure-profile=true`, the default). On servers without signed chat, or with a plugin that removes chat
signatures, the button is not shown.

## Building

Requires JDK 25 or newer and Maven.

```
mvn package
```

The jar is written to `target/`.

## Support

Ask on my Discord: https://discord.gg/nRjDw9k2vJ

## License

[MIT](LICENSE)
