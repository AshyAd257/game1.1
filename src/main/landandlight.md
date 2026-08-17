设计文档（Core Design Concept）：

1. 核心设计哲学：非极端的二元转化
   概念重构： 抛弃传统游戏里“光与暗是死敌”的刻板印象。黑暗不是邪恶，而是光的另一种表现形式。两者相生相伴，随时可以互相转化。
游戏里本质上分为三种环境，光环境，昏 暗环境（不亮也不暗，本质上是中立环境）和暗环境，这三种环境中光环境和暗环境本质上和光属性暗属性玩家涂过的地是一样的。

去UI化驱动： 不依靠生硬的UI进度条来催促玩家占领地盘，而是通过视觉舒适度来驱动玩家。玩家涂抹领地，是为了让自己的视野更清晰、更舒服。

2. 阵营非对称视觉系统（Asymmetric Vision）
   这套系统通过动态调整伽马值和曝光，让不同阵营的玩家在同一片场地上看到截然不同的世界：

光阵营玩家视角：

看敌方（暗领地）： 像密林深处的阴暗角落，光线被吞噬，视觉受限，产生扩张领地的本能冲动。

看己方（光领地）： 像正午阳光投下的明亮光斑，通透且充满安全感。

暗阵营玩家视角：

看敌方（光领地）： 过于刺眼、灼热，像过度曝光。

看己方（暗领地）： 并非漆黑一片，而是被壁炉照亮般的明亮灰调，带来室内般的温暖与平静。

然而Gamma/曝光是轻微的阵营区分+干扰,不是核心驱动力,且有上限——不能刺眼到玩家看不清对方(除非对方主动隐藏)。

3. 环境互动与材质渲染（Environment & Materials）
   没有使用破坏场景美感的“油漆”，而是让环境本身的材质随着领地属性发生改变：

半透明的客观印记： 领地划分（“墨迹”）在客观上存在，但以半透明、改变地表质感（而非覆盖贴图）的方式呈现。
墨迹本身是半透明的实体/视觉存在,不是纯粹的逻辑光源场——所以渲染上确实有"墨迹"这个东西可以看,只是它不像传统涂墨那么浓重不透明。
此外注意，玩家脱战和进入战斗状态也是两回事
玩家脱战了看到的世界更通透一些，这是为了防止玩家无法探索世界而做出的改变。也就是说，这种半透明的墨迹只在玩家进入战斗时能看到
重点：！！玩家看不到不等于它们不存在，就像前文所说，玩家看到的光影本质上都是墨迹。只是不进入战斗状态的话就不会被影响。

光领地的生态： 场地上的植物和碎石边缘呈现柔和的暖色调，模拟阳光穿透叶片的物理光照感。

暗领地的生态： 植物转为自发光，且具有固定的色相偏移（如偏冷紫、青色）。置身其中如同漫步于异化的星空，具有独特的静谧美感。

交界处张力： 光暗领地相交的边缘，会出现带有颜色的高光描边，清晰地标识出双方力量的碰撞线。

4. 动态自然领地（Dynamic Natural World）
   这是将机制融入世界观的最妙一笔——世界本身就是活的，带有天然的属性偏向：

场景自带阵营： 树阴下天然是暗领地，烈日下天然是光领地，黄昏或特定区域则是中立地带。

环境基础层:地图固有的、静态/缓慢变化的光暗分布(树荫、向阳坡地、黄昏中立带),不受玩家影响,大概是设计时铺好或者根据时间/天气系统算出来的
玩家覆盖层:战斗中由子弹/墨迹动态写入,会衰减,这是真正的"地块争夺"数据
冲突了怎么算？比如:基础层是暗属性树荫,玩家又在上面涂了一层光属性墨迹,最终视觉和判定结果该怎么算?是覆盖。被涂了光属性的话就是光属性的，反之亦然

战斗状态切换： 玩家在非战斗状态（探索时）踩入敌对的天然地块不会受到惩罚，这构成了和平探索时的“第三种状态”。一旦进入战斗，地形的天然光影就会立刻成为需要争夺和利用的战术资源。

实现想法
整体结构
还是站在现成的 SparseGridManager → GridRegion → GridCell 三层网格上改,空间索引、稀疏存储、脏标记、时间切片这些完全不动。
改动集中在 GridCell 存什么字段,以及谁来决定最终颜色这两件事。
1. GridCell 数据结构
   把现在"状态枚举直接绑死颜色"的写法拆开,变成纯客观数据:
   javaclass GridCell {
   byte ownerTeam;       // NONE / LIGHT / DARK
   float intensity;      // 1.0 衰减到 0,沿用你现成的衰减公式
   boolean ignited;      // 点燃态,逻辑不变
   }
   没被涂过的格子 ownerTeam = NONE,就是空地,这是唯一的"默认态",没有环境层可以回退。判定(减速、点燃、伤害)和渲染都从这三个字段读,逻辑跟你原来的状态机基本一致,只是把"该显示什么颜色"这件事从这个类里挪出去。
   inkCircle、坐标换算、衰减计时器、点燃降级——这些全部照搬,只是参数从"队伍0/队伍1"换成"光属性/暗属性"这个语义上的改名。

2. 子弹写入:逻辑判定 + 客观可见的墨迹,两条轨并行
   子弹命中地形后做两件事:
   java// 客观判定数据,写进网格,决定谁的地、减速加速怎么算
   gridManager.inkCircle(hitPos, weapon.getInkRadius(), bulletAttribute);

// 客观可见的墨迹实体,半透明,自己管自己的衰减和销毁
inkDecalManager.spawnDecal(hitPos, weapon.getInkRadius(), bulletAttribute);
这两者数据上是同步的(同一个半径、同一个属性、同一个衰减节奏),但渲染上分开管理:网格那一份只负责判定逻辑要用的数字,墨迹贴花负责"看得见的半透明视觉效果",包括你要的颜色描边(交界处描边可以做在 decal 的 shader 边缘,不需要等网格那边算出复杂的边界检测)。
这样分离的好处是,你现在 GridDebugRenderer 整片刷新 region 材质的逻辑可以保持不动甚至简化,因为视觉效果的精细部分(半透明、描边、衰减动画)交给独立的 decal 去处理,网格材质本身可以画得简单粗暴一些,甚至debug阶段保持现在的纯色调试块就行。

3. ColorResolver——核心新增模块
   输入三个东西,吐出最终颜色:
   javaRGBA resolve(byte ownerTeam, float intensity, byte observerFaction) {
   if (ownerTeam == NONE) return baseGroundColor; // 空地走单独分支,不参与光暗逻辑

   boolean sameSide = (ownerTeam == observerFaction);
   // sameSide=true  → 暖黄/冷蓝基色,正常亮度,微发光,边缘暖描边
   // sameSide=false → HSV remap: 亮部去饱和发灰,暗部保留/加强固有色饱和度
   //                  暗属性领地对暗属性观察者:亮灰而不是纯黑("壁炉照亮"的感觉)
   //                  光属性领地对暗属性观察者:高强度暖白,刺眼但不至于完全看不见

   // intensity 控制整体强度/透明度的衰减,贯穿整个计算
   }
   这个函数应该是纯函数,没有副作用,方便你拿几组典型输入(光看光、光看暗、暗看光、暗看暗、不同 intensity 值)单独写个小测试脚本跑出颜色直接看,不需要每次进游戏里试。

4. 战斗状态开关
   地块减速/伤害惩罚只在 inCombat == true 时生效,脱战直接跳过判定走 1.0x,不用改你现成的 getSpeedMultiplier,只在调用它的外层包一层战斗状态检查。

5. 曝光/Gamma 微调(轻量,有硬上限)
   按观察者阵营和周围领地占比算一个小幅度的曝光偏移,clamp 在一个很小的范围内,纯粹做阵营辨识度+轻微干扰,不是核心驱动机制,数值留作可调参数,实机调。

6. 环境效果的接口：留了,但目前只是"留了缝",还没有真正搭出接口骨架——我应该讲清楚区别。
   现在结构上天然留出的缝
   ownerTeam 这个字段本身就是"覆盖层专用"的语义,effectiveAttribute()(或者说现在直接等于 ownerTeam)只读这一个字段。这意味着以后要插入环境层,不需要改这个字段的含义,只需要在它旁边加一个新字段和改一行取值逻辑:
   javaclass GridCell {
   byte ownerTeam;       // 覆盖层,现在就有
   float intensity;
   boolean ignited;

   // 以后要加环境层,大概是加这个:
   // byte baseAttribute;  // 环境层,现在没有

   byte effectiveAttribute() {
   return ownerTeam;  // 现在
   // 以后: return ownerTeam != NONE ? ownerTeam : baseAttribute;
   }
   }
   这步改动小,不会推翻现有代码,纯粹加字段+改一行 if。
   但我们还没做的部分

baseAttribute 怎么算出来(静态铺地图 / 动态算光照朝向)——这套逻辑完全没写,连接口签名都没定
ColorResolver 现在的设计是"吃 ownerTeam",还没有改成"吃 effectiveAttribute()",虽然改起来是一行替换,但现在没做
没有任何"环境生成器"这个模块的雏形,哪怕是个空类或者 TODO 占位都还没放

所以准确的说法是:数据结构因为本来就是"覆盖层独立一个字段"这种干净的设计,所以事后插入环境层的成本很低、不需要推翻重做,
但这跟"已经预留了接口"不是一回事——后者意味着应该已经有个类似 interface EnvironmentAttributeProvider { byte computeBaseAttribute(int gridX, int gridZ); } 的占位存在,而现在确实一行都没有。

更新：
设计核心
同一份客观地图数据,光属性玩家和暗属性玩家看到的画面完全不同,但谁的地、该不该受惩罚,这些判定数字层面只有一份答案,不受观察者影响。领地争夺依然是认真的零和玩法,光暗只是身份+视觉语言,不是"调和不分胜负"的氛围设计。

数据结构
GridCell(替换现有 State 枚举绑定颜色的写法)
javaclass GridCell {
int factionId;         // 客观归属,查表用的key,支持任意多个独立子阵营
float intensity;       // 1.0衰减到0,沿用现成衰减公式
boolean ignited;       // 点燃态,逻辑不变
}
factionId == NONE 即空地,对应你现在的 EMPTY 状态。
FactionDef(新增,运行时查表用)
javaclass FactionDef {
int factionId;
byte visualLineage;    // LIGHT / DARK,只有这两个值,不做开放扩展
Color baseHue;         // 这个子阵营具体色相,ColorResolver 永远从这里取,不写死
}

Map<Byte, List<FactionDef>> factionsByLineage; // 现在每个血统下大概放一个默认子阵营
阵营关系判断(新增,收敛敌我判断到单一入口)
javaenum Relation { SELF, ALLY, ENEMY, NEUTRAL }

Relation getRelation(int factionA, int factionB) {
if (factionA == factionB) return Relation.SELF;
return Relation.ENEMY; // 现在简单粗暴,以后可换成查关系表,调用方不用改
}
所有判定逻辑(减速倍率、视觉滤镜的同侧判断)都通过这个函数问,不直接裸比较 factionId。
局内子阵营分配(新增,接口先留,实现先写最简单的)
javaFactionDef assignFaction(byte playerLineage, List<FactionDef> availableFactionDefs) {
return availableFactionDefs.get(0); // 现在只有一个,直接返回;以后换成随机抽取
}
分配在每局比赛开始时重新随机一次,绑定关系是局内临时数据,局结束即丢弃,不涉及存档/持久化。
环境基础层 —— 暂不实现
现在地图是一片空白起伏地形,不做树荫/向阳判定。GridCell 不加 baseAttribute 字段,等以后真要做时再补,改动成本低(加一个字段+改一行取值逻辑),现在不预留多余结构。

子弹写入:判定数据 + 客观可见墨迹,两条轨并行
javagridManager.inkCircle(hitPos, weapon.getInkRadius(), bulletFactionId); // 判定数据
inkDecalManager.spawnDecal(hitPos, weapon.getInkRadius(), bulletFactionId); // 半透明可见墨迹,自己管衰减和描边
墨迹客观存在、半透明,不是纯逻辑光源场。坐标转换、稀疏存储、inkCircle 范围计算这些底层逻辑全部照搬现有代码,只是参数语义从"队伍0/1"换成 factionId。

ColorResolver —— 核心新增模块
javaRGBA resolve(int factionId, float intensity, int observerFactionId) {
if (factionId == NONE) return baseGroundColor; // 空地走单独分支

    FactionDef cellFaction = registry.get(factionId);
    FactionDef observerFaction = registry.get(observerFactionId);
    boolean sameLineage = (cellFaction.visualLineage == observerFaction.visualLineage);
    
    // sameLineage=true  → 暖黄/冷蓝基色(取自 baseHue),正常亮度,微发光,边缘暖描边
    // sameLineage=false → HSV remap:亮部去饱和发灰,暗部保留/加强固有色饱和度
    //                     暗领地对暗观察者 = 亮灰("壁炉感"),不是纯黑
    //                     光领地对暗观察者 = 高强度暖白,刺眼但有上限,不至于完全看不见
    // intensity 贯穿整个计算,控制强度/透明度衰减
}
纯函数,无副作用,可以脱离游戏单独写测试脚本调参。判断"该用哪套滤镜"看 visualLineage(只关心光暗血统),不看具体是哪个子阵营——子阵营再多,这个函数不用改。

战斗状态开关
地块惩罚(减速/伤害)只在 inCombat == true 时生效,脱战直接 1.0x,不改 getSpeedMultiplier 本身,只在调用处包一层战斗状态判断。
曝光/Gamma 微调(轻量,有硬上限)
按观察者血统+周围领地构成算一个小幅度曝光偏移,clamp 在很小范围内。纯粹是阵营辨识度+轻微干扰,不是核心驱动机制,且必须保证对方非隐藏状态下始终可见。数值留作可调参数,实机调。

实现顺序

GridCell 字段改名拆分(地基,改动小但所有后续依赖它)
FactionDef + FactionRegistry + getRelation()(把阵营查表和关系判断的骨架搭起来,哪怕现在只有2个子阵营)
ColorResolver 核心算法(先只测网格直接刷色,不接 decal,最快验证视觉效果)
子弹改为同时调用 inkCircle + spawnDecal(墨迹独立实体化,加描边)
曝光/gamma 微调(最后做,纯数值调参)

环境基础层(Layer 1)不在这次范围内,留空。