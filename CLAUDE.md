# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.


## UI Redesign Guidelines

### Audit first, fix second
Before touching any layout file:
1. List every screen that has the issue
2. Check if there's a shared component to fix instead of N files
3. Fix the component, not the instances

### Navigation rules
- Bottom nav: 3 tabs (Bàn · Đơn hàng · Menu) — keep as-is, it works
- Screens WITHOUT bottom nav: all admin screens, order flow (Menu→Order→Payment→Invoice)
- Back button: ALWAYS visible on non-entry screens. Entry screens = TableActivity, LoginActivity
- Admin access: via top_bar btn_right (gear icon), NOT bottom nav

### Button placement rules
- Primary action (Xác nhận, Thanh toán...): component_bottom_total_panel, pinned to bottom
- Destructive actions (Huỷ đơn): text button, top right or inline — never primary style
- FAB: only on list screens for "add" actions (AdminMenu, UserManagement, PromotionManagement)

### Visual consistency checklist
- All cards: bg_card drawable, spacing_lg padding
- All list items: consistent height, no orphaned buttons at random Y positions  
- Status badges: only use bg_badge_success / bg_badge_warning / bg_badge_notification
- Icons: stick to existing drawables — do not introduce new icon sets mid-project

## Promotion Engine (TODO - post backend)

1. Bulk discount (mua nhiều giảm nhiều)
   - Tier-based: buy X get Y% off
   - Apply per product or per category

2. Product-specific promotion  
   - Discount on specific product IDs
   - Buy A get B free (BOGO)

3. Order total discount
   - Min order value threshold
   - Percentage or fixed amount off

4. Loyalty / Khách hàng thân thiết
   - Customer entity: phone, name, points, tier
   - Earn points per order (configurable rate)
   - Redeem points as discount
   - Tier levels: Bronze / Silver / Gold

## Backend considerations
- Promotion stacking rules (can promotions combine?)
- Promotion priority when multiple apply
- Points expiry policy
- Customer lookup by phone at POS