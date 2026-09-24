# physai-isco-3314 — 統計・数理の準専門職（ISCO 3314）のデータ受付・報告書印刷ロボットの physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isco-3314`、ISCO 3314 統計・数理の準専門職）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: データ受付・報告書印刷ロボットがデータセットのスキャン・報告書の製本・物理保管を行う。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:hot-melt-spine-binding` | thermal | 製本機のプラテン（180 °C 固定）が背の 2 mm ホットメルト糊を溶かす。紙側の糊面温度（紙側は断熱と置く） | 紙側の糊面温度 | 120 °C 以上（estimate） |
| `:bound-reports-to-archive` | manipulator | 製本済みの報告書の束を製本機出口から保管棚へ持ち上げる | 肩関節ピークトルク | 45 N·m（estimate） |

測定の入口: `kbb -M:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:test`（`test/statanalysis/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する）。
この repo 自身の `.kotoba` test は kbb では走らない（fleet の JVM gate が走らせる）。この bot の test 数は physics の test だけを数える。

## 測って分かったこと・限界（成長の第一候補）

1. **ホットメルト製本**: 紙側の糊面温度は加熱 10 s で 75.24 °C（限界未達）、20 s で 125.23 °C、30 s で 151.41 °C、60 s で 175.93 °C、90 s で 179.42 °C。
   120 °C に達するのは **18.6 s**（boundary 18.59 s）。これより短い加熱では紙の小口まで糊が流動せず、製本が剥がれる側に倒れる。
   厚さ 2 mm・熱拡散率 1.05×10⁻⁷ m²/s の拡散時間（約 38 s）が支配している。
2. **報告書の束**: 肩トルクは 0.5 kg で 20.97 N·m、2 kg で 28.89 N·m、4 kg で 39.53 N·m、6 kg で 50.20 N·m（限界超過）。限界 45 N·m に達する積荷は **5.025 kg**。
3. **estimate のままの値**: 糊の作業溶融温度 120 °C（製本用 EVA ホットメルトの製品データシートで置き換える）、糊の熱物性（k 0.2・密度 950・比熱 2000）、
   プラテン温度 180 °C、肩トルク上限 45 N·m（協働アームの仕様書で置き換える）。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isco-3314 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:test → kbb -M:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isco-3314 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
