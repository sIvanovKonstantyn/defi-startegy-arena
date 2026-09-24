# DeFi Arena information architecture

## Core object

**Strategy** — identity, privacy, versions, configuration, risk, performance, runs.

## Scalable nav (target)

```text
DeFi Arena
├── Strategies (All / Mine / Templates)
├── Arena (Live / Simulations / Leaderboard)
├── Analytics
└── Settings
```

Current prototype exposes Strategies + Auth only. New areas must plug into this shell rather than inventing parallel navigation.

## Page questions

| Page | Primary question | Primary action |
| --- | --- | --- |
| Sign in | Who am I? | Sign in |
| Strategies | What strategies do I have? | New strategy |
| Strategy detail (future) | What is this strategy and how does it perform? | Run simulation / Edit |
| Arena (future) | How are strategies performing in simulation? | Join / Inspect |

## Shell

Persistent header: brand home link (left), account / logout (right). Future: primary nav under brand.
