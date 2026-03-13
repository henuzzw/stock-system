# DEV_CHECKLIST.md

> 2026-03-12 临时开发清单（按当前优先级）

- [x] 添加股票后自动初始化基础数据（至少补 daily/fundamentals/technicals，A股补 minute）
  - 备注：minute 步骤依赖外部数据源（AkShare/东财），当前环境如代理不可用可能失败；日线/指标可正常补齐。
- [ ] 新版策略文档同步（strategy.yml / HANDOFF.md / DEV_CHECKLIST.md）
- [ ] D7 买入逻辑升级：盘中信号触发，不再固定 10:10；要求 trend_ok=1、价格>SMA20，并增加 RSI / 过热过滤
- [ ] D7 卖出逻辑升级：盘中信号触发，不再固定 14:50；支持 -8% 止损、连续2天跌出Top10、连续2天 trend_ok=0 / 收盘<SMA20、浮盈回撤止盈
- [ ] 分钟线自动采集（候选池优先）
- [ ] 账户页（现金/持仓/订单/成交）
- [ ] 策略页（今日计划/执行结果/跳过原因）
- [ ] 日期字段去时区歧义（trade_date/run_date 直接返回 YYYY-MM-DD）
