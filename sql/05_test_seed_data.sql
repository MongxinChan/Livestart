-- =============================================================================
-- 🎫 LiveStart 测试种子数据脚本 (sql/05_test_seed_data.sql)
-- =============================================================================
--
-- 本文件由 populate_events.py 数据模板确定性转换生成，固定 ID 可重复执行。
--
-- 覆盖范围：
--   - 音乐风格字典 t_style              (17 条)
--   - 场馆          t_venue             (10 个)
--   - 艺人          t_performer         (16 位)
--   - 艺人-风格关联  t_performer_style_relation
--   - 演出          t_event             (35 场)
--   - 演出-风格关联  t_event_style_relation
--   - 演出配置       t_event_config      (35 条)
--   - 票档          t_ticket_sku         (演唱会6档 / Livehouse4档)
--   - 用户/订单基础测试数据 (12乐迷 + 4艺人账户 + 4主办方 + 8笔订单)
--
-- 关键约束：
--   - ID 全部使用固定可读整数 (10xxxx / 20xxxx / 30xxxx 区段)
--   - 全部 INSERT 使用 ON DUPLICATE KEY UPDATE 实现幂等，可重复执行
--   - 密码统一为 BCrypt('LiveStart123')
--
-- 已知限制：
--   - 本脚本不预热 Redis 库存，对这些演出走"真实抢票流程"会失败
--     (只用于 UI 数据展示 / 用户管理 / 订单管理 / 演出管理 列表)
--   - t_event / t_ticket_sku 是广播表，脚本会在所有相关库中重复 INSERT 同步
--
-- 执行：mysql -uroot -p123456 < sql/05_test_seed_data.sql
-- 清理：使用脚本末尾注释的 DELETE 段（默认注释掉，避免误删）
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `live_start`;

-- =============================================================================
-- 0. 音乐风格字典 t_style
--    style_id 对应 Python 脚本里 PERFORMERS 的 styles 列表数值
-- =============================================================================
INSERT INTO `t_style` (`id`, `name`, `code`, `description`) VALUES
  (1, '流行', 'POP', '当代流行音乐，旋律优美、节奏明快'),
  (2, '华语流行', 'CPOP', '华语地区主流流行音乐'),
  (3, '欧美流行', 'WESTERN_POP', '欧美地区主流流行音乐'),
  (4, '日韩流行', 'JPKPOP', '日本与韩国流行音乐（J-POP / K-POP）'),
  (5, '摇滚', 'ROCK', '以吉他、贝斯、鼓为核心的摇滚乐'),
  (6, '独立摇滚', 'INDIE_ROCK', '独立厂牌发行的另类摇滚风格'),
  (7, '朋克', 'PUNK', '快速节奏、简洁编曲的朋克摇滚'),
  (8, '金属', 'METAL', '重型吉他 riff 与高强度打击乐'),
  (9, '嘻哈', 'HIPHOP', '说唱与节拍驱动的嘻哈音乐'),
  (10, '说唱', 'RAP', '以韵律念白为主的说唱表演'),
  (11, '电子', 'ELECTRONIC', '电子合成器与数字制作的音乐风格'),
  (12, 'EDM', 'EDM', '电子舞曲，适合大型音乐节'),
  (13, 'Techno', 'TECHNO', '重复性节拍驱动的电子音乐'),
  (14, 'House', 'HOUSE', '四拍底鼓驱动的电子舞曲'),
  (15, 'R&B', 'RNB', '节奏布鲁斯，融合灵魂与流行元素'),
  (16, '灵魂乐', 'SOUL', '源自福音音乐的深情演唱风格'),
  (17, '民谣', 'FOLK', '原声吉他与叙事性歌词的民谣音乐'),
  (18, '乡村', 'COUNTRY', '美式乡村音乐风格'),
  (19, '爵士', 'JAZZ', '即兴演奏与复杂和声的爵士乐'),
  (20, '布鲁斯', 'BLUES', '源自非裔美国人音乐传统的蓝调'),
  (21, '古典', 'CLASSICAL', '西方古典音乐，含交响乐与室内乐'),
  (22, '民族', 'ETHNIC', '融合各民族传统元素的音乐风格'),
  (23, '国风', 'CHINESE_STYLE', '融合中国传统乐器与现代编曲'),
  (24, '戏曲', 'OPERA', '中国传统戏曲艺术表演'),
  (25, '雷鬼', 'REGGAE', '牙买加风格的节奏音乐'),
  (26, '拉丁', 'LATIN', '拉丁美洲风格 of 音乐与舞蹈'),
  (27, 'Bossa Nova', 'BOSSA_NOVA', '巴西风格的柔和爵士融合'),
  (28, '脱口秀', 'TALK_SHOW', '单人或多人喜剧脱口秀表演'),
  (29, '相声', 'CROSSTALK', '中国传统语言类曲艺表演'),
  (30, '音乐剧', 'MUSICAL', '融合歌唱、对白与舞蹈的舞台剧'),
  (31, '话剧', 'DRAMA', '以对话和表演为主的戏剧'),
  (32, '舞蹈', 'DANCE', '各类舞蹈表演（现代舞、芭蕾等）'),
  (33, '魔术', 'MAGIC', '魔术与幻术表演'),
  (34, '综合演出', 'VARIETY', '多种表演形式混合的综艺类演出')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `code` = VALUES(`code`);

-- =============================================================================
-- 1. 场馆 t_venue  (id: 101001 ~ 101010，对应 VENUES 列表顺序)
-- =============================================================================
INSERT INTO `t_venue` (`id`, `name`, `city`, `address`, `capacity`) VALUES
  (101001, '北京国家体育场 (鸟巢)',          '北京', '北京市朝阳区国家体育场南路1号',                     80000),
  (101002, '上海梅赛德斯-奔驰文化中心',       '上海', '上海市浦东新区世博大道1500号',                     18000),
  (101003, '广州天河体育中心体育场',          '广州', '广州市天河区天河路299号',                          50000),
  (101004, '杭州奥体中心体育场 (大莲花)',     '杭州', '杭州市滨江区飞虹路',                               80000),
  (101005, '成都凤凰山体育公园综合体育馆',    '成都', '成都市金牛区北星大道二段',                         18000),
  (101006, '深圳 HOU Livehouse',             '深圳', '深圳市福田区下沙KK ONE购物中心B1层',                  800),
  (101007, '上海 Modern Sky LAB',            '上海', '上海市虹口区瑞虹路188号瑞虹天地月亮湾3F',            1000),
  (101008, '北京 MAO Livehouse (五棵松)',     '北京', '北京市海淀区复兴路69号华熙LIVE·五棵松G-23',           800),
  (101009, '杭州九莱福音乐现场',              '杭州', '杭州市拱墅区新北街85号三层301',                     1200),
  (101010, '广州太空间 Livehouse',            '广州', '广州市海珠区革新路124号太古仓沙面工业区5号仓',       1500)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `capacity` = VALUES(`capacity`);

-- =============================================================================
-- 2. 艺人 t_performer  (id: 102001 ~ 102016，对应 PERFORMERS 列表顺序)
-- =============================================================================
INSERT INTO `t_performer` (`id`, `name`, `style_id`, `avatar`, `bio`, `status`) VALUES
  (102001, '周杰伦',     2,  'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&q=80&w=300', '华语流行乐坛领军人物，创作歌手，音乐制作人，金曲奖得主。其独特的中国风与中西融合风格开创了华语乐坛新纪元。', 1),
  (102002, '陈奕迅',     2,  'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=300', '华语乐坛实力派唱将，被誉为"歌神"继承人，其磁性深情的嗓音和极具故事感的歌曲唱尽人世百态。', 1),
  (102003, '林俊杰',     2,  'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&q=80&w=300', '行走的CD，著名流行音乐创作歌手。高超的唱功与优秀的编曲制作实力使其长青于华语流行前沿。', 1),
  (102004, '邓紫棋',     2,  'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&q=80&w=300', '铁肺歌后，华语乐坛具有全球影响力的唱作女歌手，嗓音爆发力惊人，创作金曲无数。', 1),
  (102005, '薛之谦',     2,  'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&q=80&w=300', '知名流行创作歌手，以"薛氏情歌"风靡华语地区，独特的幽默段子与深情情歌形成强烈反差。', 1),
  (102006, '五月天',     2,  'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&q=80&w=300', '亚洲传奇摇滚乐团，成军二十余载，用充满青春、梦想与热血的赞歌串联起无数人的青春印记。', 1),
  (102007, '万能青年旅店', 5, 'https://images.unsplash.com/photo-1487180142328-054b783fc471?auto=format&fit=crop&q=80&w=300', '石家庄独立摇滚标杆，极其深邃和极富诗意的歌词，融合爵士与前卫摇滚的管乐编曲，是中国独立音乐的里程碑。', 1),
  (102008, '告五人',     2,  'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&q=80&w=300', '新锐独立流行乐队，以魔性的旋律 and 治愈系男女双主唱曲风在年轻人中迅速走红。', 1),
  (102009, '新裤子',     5,  'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=300', '中国复古迪斯科与新浪潮摇滚代表乐团，魔性摇摆的舞台台风和直白感人的歌词让人热泪盈眶。', 1),
  (102010, '痛仰乐队',   5,  'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&q=80&w=300', '中国最具现场号召力的摇滚乐队之一，从重金属硬核转向公路雷鬼摇滚，高唱着"一直往南方开"。', 1),
  (102011, '回春丹',     5,  'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&q=80&w=300', '来自广西钦州的独立摇滚乐队，以复古迷幻的吉他 Riff 和主唱妖娆独特的嗓音著称，极具现场感染力。', 1),
  (102012, '重塑雕像的权利', 5, 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=300', '中国后朋克与电子摇滚美学大师，编曲严谨细致如精密仪器运转，舞台效果震撼人心。', 1),
  (102013, '赵雷',       17, 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&q=80&w=300', '独立民谣歌手，嗓音朴实真诚，用细腻接地气的歌词唱出平凡生活里的感动与无奈。代表作《成都》。', 1),
  (102014, '房东的猫',   17, 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&q=80&w=300', '快乐的民谣双人女声组合，清澈干净的治愈系女声，宛如山间清爽的风，温暖抚平人心。', 1),
  (102015, 'GAI周延',    9,  'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=300', '新华流说唱领军人物，独树一帜的"江湖流"中国风说唱，将侠义豪情融入激昂的说唱节奏。', 1),
  (102016, '马思唯',     9,  'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=300', 'Higher Brothers 团队核心，国际知名华语说唱歌手，Flow 华丽，被誉为中文说唱天才标杆。', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `bio` = VALUES(`bio`);

-- =============================================================================
-- 3. 艺人-风格关联 t_performer_style_relation
-- =============================================================================
INSERT IGNORE INTO `t_performer_style_relation` (`performer_id`, `style_id`) VALUES
  -- 周杰伦 [2,1]
  (102001, 2), (102001, 1),
  -- 陈奕迅 [2,1]
  (102002, 2), (102002, 1),
  -- 林俊杰 [2,1]
  (102003, 2), (102003, 1),
  -- 邓紫棋 [2,1]
  (102004, 2), (102004, 1),
  -- 薛之谦 [2,1]
  (102005, 2), (102005, 1),
  -- 五月天 [2,5]
  (102006, 2), (102006, 5),
  -- 万能青年旅店 [5,6]
  (102007, 5), (102007, 6),
  -- 告五人 [2,6]
  (102008, 2), (102008, 6),
  -- 新裤子 [5,6]
  (102009, 5), (102009, 6),
  -- 痛仰乐队 [5]
  (102010, 5),
  -- 回春丹 [5,6]
  (102011, 5), (102011, 6),
  -- 重塑雕像的权利 [5,6]
  (102012, 5), (102012, 6),
  -- 赵雷 [17]
  (102013, 17),
  -- 房东的猫 [17]
  (102014, 17),
  -- GAI周延 [9,10,23]
  (102015, 9), (102015, 10), (102015, 23),
  -- 马思唯 [9,10]
  (102016, 9), (102016, 10);

-- =============================================================================
-- 4. 演出 t_event + 演出配置 t_event_config + 票档 t_ticket_sku
--    id: 103001 ~ 103035 (35 场)
--    海报轮询 POSTERS[i % 10]，场馆/艺人按脚本逻辑分配确定性版本
--    演唱会 (capacity > 10000)：6档票 / Livehouse：4档票
-- =============================================================================

-- -----------------------------------------------------------------------
-- 场馆容量速查（用于决定 event_type 和票档）:
--   大型 (>10000): 101001(8w) 101002(1.8w) 101003(5w) 101004(8w) 101005(1.8w)
--   Livehouse:     101006(800) 101007(1000) 101008(800) 101009(1200) 101010(1500)
-- -----------------------------------------------------------------------

-- ---- 演出 1: 周杰伦 × 北京鸟巢 (演唱会) ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103001, '「周杰伦」2026"嘉年华"世界巡回演唱会 - 北京站', 1, 101001, '2026-07-12 19:30:00',
   'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103001, 0, 1, 4, 2, 48, 24, 0.20, 1, 1)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200001, 103001, '看台 380', 380, 380, 12000, 12000, 6, 0),
  (200002, 103001, '看台 580', 580, 580, 20000, 20000, 6, 0),
  (200003, 103001, '看台 780', 780, 780, 16000, 16000, 6, 0),
  (200004, 103001, '看台 980', 980, 980, 12000, 12000, 6, 0),
  (200005, 103001, '内场 1380', 1380, 1380, 12000, 9000, 6, 0),
  (200006, 103001, '内场 1680 (极速抢票)', 1680, 1680, 8000, 1000, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103001, 1), (103001, 2);

-- ---- 演出 2: 陈奕迅 × 上海梅奔 (演唱会) ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103002, '「陈奕迅」"FEAR and DREAMS"2026 新年特别专场演唱会 - 上海站', 1, 101002, '2026-08-03 19:30:00',
   'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103002, 0, 1, 4, 2, 48, 24, 0.20, 1, 1)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200011, 103002, '看台 380', 380, 380, 2700, 2700, 6, 0),
  (200012, 103002, '看台 580', 580, 580, 4500, 4500, 6, 0),
  (200013, 103002, '看台 780', 780, 780, 3600, 3600, 6, 0),
  (200014, 103002, '看台 980', 980, 980, 2700, 2700, 6, 0),
  (200015, 103002, '内场 1380', 1380, 1380, 2700, 1200, 6, 0),
  (200016, 103002, '内场 1680 (极速抢票)', 1680, 1680, 1800, 0, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103002, 1), (103002, 2);

-- ---- 演出 3: 林俊杰 × 广州天河 (演唱会) ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103003, '「林俊杰」2026"JJ20"巡回双人间音乐盛典 - 广州站', 1, 101003, '2026-08-22 19:30:00',
   'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103003, 0, 1, 4, 2, 48, 24, 0.20, 1, 1)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200021, 103003, '看台 380', 380, 380, 7500, 7500, 6, 0),
  (200022, 103003, '看台 580', 580, 580, 12500, 12500, 6, 0),
  (200023, 103003, '看台 780', 780, 780, 10000, 10000, 6, 0),
  (200024, 103003, '看台 980', 980, 980, 7500, 7500, 6, 0),
  (200025, 103003, '内场 1380', 1380, 1380, 7500, 5000, 6, 0),
  (200026, 103003, '内场 1680 (极速抢票)', 1680, 1680, 5000, 500, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103003, 1), (103003, 2);

-- ---- 演出 4: 邓紫棋 × 杭州大莲花 (演唱会) ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103004, '「邓紫棋」"I AM GLORIA"2026 新年特别专场演唱会 - 杭州站', 1, 101004, '2026-09-06 19:30:00',
   'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103004, 0, 1, 4, 2, 48, 24, 0.20, 1, 1)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200031, 103004, '看台 380', 380, 380, 12000, 12000, 6, 0),
  (200032, 103004, '看台 580', 580, 580, 20000, 20000, 6, 0),
  (200033, 103004, '看台 780', 780, 780, 16000, 16000, 6, 0),
  (200034, 103004, '看台 980', 980, 980, 12000, 12000, 6, 0),
  (200035, 103004, '内场 1380', 1380, 1380, 12000, 12000, 6, 0),
  (200036, 103004, '内场 1680 (极速抢票)', 1680, 1680, 8000, 8000, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103004, 1), (103004, 2);

-- ---- 演出 5: 薛之谦 × 成都凤凰山 (演唱会) ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103005, '「薛之谦」2026"天外来物"世界巡回演唱会 - 成都站', 1, 101005, '2026-09-20 19:30:00',
   'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103005, 0, 1, 4, 2, 48, 24, 0.20, 1, 1)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200041, 103005, '看台 380', 380, 380, 2700, 2500, 6, 0),
  (200042, 103005, '看台 580', 580, 580, 4500, 4000, 6, 0),
  (200043, 103005, '看台 780', 780, 780, 3600, 3000, 6, 0),
  (200044, 103005, '看台 980', 980, 980, 2700, 2000, 6, 0),
  (200045, 103005, '内场 1380', 1380, 1380, 2700, 1500, 6, 0),
  (200046, 103005, '内场 1680 (极速抢票)', 1680, 1680, 1800, 200, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103005, 1), (103005, 2);

-- ---- 演出 6: 五月天 × 北京鸟巢 (演唱会) ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103006, '「五月天」"回到那一天"2026 新年特别专场演唱会 - 北京站', 1, 101001, '2026-10-04 19:30:00',
   'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103006, 0, 1, 4, 2, 48, 24, 0.20, 1, 1)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200051, 103006, '看台 380', 380, 380, 12000, 11000, 6, 0),
  (200052, 103006, '看台 580', 580, 580, 20000, 18000, 6, 0),
  (200053, 103006, '看台 780', 780, 780, 16000, 14000, 6, 0),
  (200054, 103006, '看台 980', 980, 980, 12000, 9000, 6, 0),
  (200055, 103006, '内场 1380', 1380, 1380, 12000, 5000, 6, 0),
  (200056, 103006, '内场 1680 (极速抢票)', 1680, 1680, 8000, 0, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103006, 1), (103006, 5);

-- ---- 演出 7: 万能青年旅店 × 深圳 HOU Livehouse ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103007, '「万能青年旅店」"河北墨麒麟"2026 Livehouse 极致声学巡演 - 深圳站', 0, 101006, '2026-07-19 20:00:00',
   'https://images.unsplash.com/photo-1487180142328-054b783fc471?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103007, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200061, 103007, '学生预售票',       150, 150, 120,  80, 6, 0),
  (200062, 103007, '全价预售票',       220, 220, 400, 250, 6, 0),
  (200063, 103007, '现场全价票',       280, 280, 160,  80, 6, 0),
  (200064, 103007, 'VIP 门票 (含优先入场)', 480, 480, 120,  30, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103007, 5), (103007, 6);

-- ---- 演出 8: 告五人 × 上海 Modern Sky LAB ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103008, '「告五人」"宇宙超有趣"特别限定Live专场 - 上海站', 0, 101007, '2026-07-26 20:00:00',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103008, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200071, 103008, '学生预售票',       150, 150, 150, 100, 6, 0),
  (200072, 103008, '全价预售票',       220, 220, 500, 350, 6, 0),
  (200073, 103008, '现场全价票',       280, 280, 200, 100, 6, 0),
  (200074, 103008, 'VIP 门票 (含优先入场)', 480, 480, 150,  50, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103008, 1), (103008, 6);

-- ---- 演出 9: 新裤子 × 北京 MAO Livehouse ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103009, '「新裤子」"我们是一伙的"2026 Livehouse 极致声学巡演 - 北京站', 0, 101008, '2026-08-09 20:00:00',
   'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103009, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200081, 103009, '学生预售票',       150, 150, 120,  60, 6, 0),
  (200082, 103009, '全价预售票',       220, 220, 400, 180, 6, 0),
  (200083, 103009, '现场全价票',       280, 280, 160,  50, 6, 0),
  (200084, 103009, 'VIP 门票 (含优先入场)', 480, 480, 120,  10, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103009, 5), (103009, 6);

-- ---- 演出 10: 痛仰乐队 × 杭州九莱福 ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103010, '「痛仰乐队」"一直往南方开"特别限定Live专场 - 杭州站', 0, 101009, '2026-08-16 20:00:00',
   'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103010, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200091, 103010, '学生预售票',       150, 150, 180, 150, 6, 0),
  (200092, 103010, '全价预售票',       220, 220, 600, 500, 6, 0),
  (200093, 103010, '现场全价票',       280, 280, 240, 200, 6, 0),
  (200094, 103010, 'VIP 门票 (含优先入场)', 480, 480, 180, 120, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103010, 5);

-- ---- 演出 11: 回春丹 × 广州太空间 ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103011, '「回春丹」"开炉"2026 Livehouse 极致声学巡演 - 广州站', 0, 101010, '2026-08-23 20:00:00',
   'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103011, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200101, 103011, '学生预售票',       150, 150, 225, 225, 6, 0),
  (200102, 103011, '全价预售票',       220, 220, 750, 750, 6, 0),
  (200103, 103011, '现场全价票',       280, 280, 300, 300, 6, 0),
  (200104, 103011, 'VIP 门票 (含优先入场)', 480, 480, 225, 225, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103011, 5), (103011, 6);

-- ---- 演出 12: 重塑雕像的权利 × 深圳 HOU Livehouse ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103012, '「重塑雕像的权利」"精密几何"特别限定Live专场 - 深圳站', 0, 101006, '2026-09-05 20:00:00',
   'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103012, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200111, 103012, '学生预售票',       150, 150, 120,  40, 6, 0),
  (200112, 103012, '全价预售票',       220, 220, 400,  80, 6, 0),
  (200113, 103012, '现场全价票',       280, 280, 160,  20, 6, 0),
  (200114, 103012, 'VIP 门票 (含优先入场)', 480, 480, 120,   5, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103012, 5), (103012, 6);

-- ---- 演出 13: 赵雷 × 上海 Modern Sky LAB ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103013, '「赵雷」"成都"2026 Livehouse 极致声学巡演 - 上海站', 0, 101007, '2026-09-13 20:00:00',
   'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103013, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200121, 103013, '学生预售票',       150, 150, 150, 120, 6, 0),
  (200122, 103013, '全价预售票',       220, 220, 500, 420, 6, 0),
  (200123, 103013, '现场全价票',       280, 280, 200, 160, 6, 0),
  (200124, 103013, 'VIP 门票 (含优先入场)', 480, 480, 150, 100, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103013, 17);

-- ---- 演出 14: 房东的猫 × 北京 MAO Livehouse ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103014, '「房东的猫」"世界一次次被治愈"特别限定Live专场 - 北京站', 0, 101008, '2026-09-19 20:00:00',
   'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103014, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200131, 103014, '学生预售票',       150, 150, 120, 120, 6, 0),
  (200132, 103014, '全价预售票',       220, 220, 400, 400, 6, 0),
  (200133, 103014, '现场全价票',       280, 280, 160, 160, 6, 0),
  (200134, 103014, 'VIP 门票 (含优先入场)', 480, 480, 120, 120, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103014, 17);

-- ---- 演出 15: GAI周延 × 杭州九莱福 ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103015, '「GAI周延」"大逆有道"2026 Livehouse 极致声学巡演 - 杭州站', 0, 101009, '2026-09-27 20:00:00',
   'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103015, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200141, 103015, '学生预售票',       150, 150, 180, 100, 6, 0),
  (200142, 103015, '全价预售票',       220, 220, 600, 380, 6, 0),
  (200143, 103015, '现场全价票',       280, 280, 240, 150, 6, 0),
  (200144, 103015, 'VIP 门票 (含优先入场)', 480, 480, 180,  80, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103015, 9), (103015, 10);

-- ---- 演出 16: 马思唯 × 广州太空间 ----
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103016, '「马思唯」"Masiwei World Tour"特别限定Live专场 - 广州站', 0, 101010, '2026-10-11 20:00:00',
   'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);
INSERT INTO `t_event_config` (`event_id`,`selection_mode`,`is_verify_required`,`max_tickets_per_user`,`refund_policy_type`,`tier1_free_refund_hours`,`tier2_partial_refund_hours`,`tier2_refund_fee_rate`,`is_transferable`,`is_waiting_allowed`)
  VALUES (103016, 0, 0, 6, 1, 24, 0, 0.00, 1, 0)
  ON DUPLICATE KEY UPDATE `max_tickets_per_user` = VALUES(`max_tickets_per_user`);
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200151, 103016, '学生预售票',       150, 150, 225, 225, 6, 0),
  (200152, 103016, '全价预售票',       220, 220, 750, 750, 6, 0),
  (200153, 103016, '现场全价票',       280, 280, 300, 300, 6, 0),
  (200154, 103016, 'VIP 门票 (含优先入场)', 480, 480, 225, 225, 6, 0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` (`event_id`, `style_id`) VALUES (103016, 9), (103016, 10);

-- ---- 演出 17~35: 补充 19 场，保证凑满 35 场 ----

-- 17: 周杰伦 × 上海梅奔
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103017, '「周杰伦」"魔天伦"2026 新年特别专场演唱会 - 上海站', 1, 101002, '2026-10-18 19:30:00',
   'https://images.unsplash.com/photo-1487180142328-054b783fc471?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103017, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200161,103017,'看台 380',380,380,2700,1500,6,0),(200162,103017,'看台 580',580,580,4500,2000,6,0),
  (200163,103017,'看台 780',780,780,3600,1000,6,0),(200164,103017,'看台 980',980,980,2700,500,6,0),
  (200165,103017,'内场 1380',1380,1380,2700,200,6,0),(200166,103017,'内场 1680 (极速抢票)',1680,1680,1800,0,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103017,1),(103017,2);

-- 18: 陈奕迅 × 广州天河
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103018, '「陈奕迅」"DUO"2026 巡回双人间音乐盛典 - 广州站', 1, 101003, '2026-10-25 19:30:00',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103018, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200171,103018,'看台 380',380,380,7500,7500,6,0),(200172,103018,'看台 580',580,580,12500,12500,6,0),
  (200173,103018,'看台 780',780,780,10000,10000,6,0),(200174,103018,'看台 980',980,980,7500,7500,6,0),
  (200175,103018,'内场 1380',1380,1380,7500,7500,6,0),(200176,103018,'内场 1680 (极速抢票)',1680,1680,5000,5000,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103018,1),(103018,2);

-- 19: 林俊杰 × 杭州大莲花
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103019, '「林俊杰」"圣所"2026 世界巡回演唱会 - 杭州站', 1, 101004, '2026-11-01 19:30:00',
   'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103019, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200181,103019,'看台 380',380,380,12000,12000,6,0),(200182,103019,'看台 580',580,580,20000,20000,6,0),
  (200183,103019,'看台 780',780,780,16000,16000,6,0),(200184,103019,'看台 980',980,980,12000,12000,6,0),
  (200185,103019,'内场 1380',1380,1380,12000,12000,6,0),(200186,103019,'内场 1680 (极速抢票)',1680,1680,8000,8000,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103019,1),(103019,2);

-- 20: 邓紫棋 × 北京鸟巢
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103020, '「邓紫棋」"Queen of Hearts"2026 世界巡回演唱会 - 北京站', 1, 101001, '2026-11-08 19:30:00',
   'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103020, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200191,103020,'看台 380',380,380,12000,12000,6,0),(200192,103020,'看台 580',580,580,20000,20000,6,0),
  (200193,103020,'看台 780',780,780,16000,16000,6,0),(200194,103020,'看台 980',980,980,12000,12000,6,0),
  (200195,103020,'内场 1380',1380,1380,12000,12000,6,0),(200196,103020,'内场 1680 (极速抢票)',1680,1680,8000,8000,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103020,1),(103020,2);

-- 21: 五月天 × 广州天河
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103021, '「五月天」"好好好想见到你"2026 世界巡回演唱会 - 广州站', 1, 101003, '2026-11-15 19:30:00',
   'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103021, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200201,103021,'看台 380',380,380,7500,7500,6,0),(200202,103021,'看台 580',580,580,12500,12500,6,0),
  (200203,103021,'看台 780',780,780,10000,10000,6,0),(200204,103021,'看台 980',980,980,7500,7500,6,0),
  (200205,103021,'内场 1380',1380,1380,7500,7500,6,0),(200206,103021,'内场 1680 (极速抢票)',1680,1680,5000,5000,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103021,1),(103021,5);

-- 22: 万能青年旅店 × 北京 MAO
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103022, '「万能青年旅店」"冀西南林歌"特别限定Live专场 - 北京站', 0, 101008, '2026-11-22 20:00:00',
   'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103022, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200211,103022,'学生预售票',150,150,120,120,6,0),(200212,103022,'全价预售票',220,220,400,400,6,0),
  (200213,103022,'现场全价票',280,280,160,160,6,0),(200214,103022,'VIP 门票 (含优先入场)',480,480,120,120,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103022,5),(103022,6);

-- 23: 告五人 × 深圳 HOU
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103023, '「告五人」"在这座城市遇见了你"2026 Livehouse 极致声学巡演 - 深圳站', 0, 101006, '2026-11-29 20:00:00',
   'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103023, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200221,103023,'学生预售票',150,150,120,120,6,0),(200222,103023,'全价预售票',220,220,400,400,6,0),
  (200223,103023,'现场全价票',280,280,160,160,6,0),(200224,103023,'VIP 门票 (含优先入场)',480,480,120,120,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103023,1),(103023,6);

-- 24: 新裤子 × 上海 Modern Sky LAB
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103024, '「新裤子」"新浪潮狂欢"特别限定Live专场 - 上海站', 0, 101007, '2026-12-06 20:00:00',
   'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103024, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200231,103024,'学生预售票',150,150,150,150,6,0),(200232,103024,'全价预售票',220,220,500,500,6,0),
  (200233,103024,'现场全价票',280,280,200,200,6,0),(200234,103024,'VIP 门票 (含优先入场)',480,480,150,150,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103024,5),(103024,6);

-- 25: 痛仰乐队 × 广州太空间
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103025, '「痛仰乐队」"百限聚乐部"2026 Livehouse 极致声学巡演 - 广州站', 0, 101010, '2026-12-13 20:00:00',
   'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103025, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200241,103025,'学生预售票',150,150,225,225,6,0),(200242,103025,'全价预售票',220,220,750,750,6,0),
  (200243,103025,'现场全价票',280,280,300,300,6,0),(200244,103025,'VIP 门票 (含优先入场)',480,480,225,225,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103025,5);

-- 26: 回春丹 × 杭州九莱福
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103026, '「回春丹」"鲜花之盛"特别限定Live专场 - 杭州站', 0, 101009, '2026-12-20 20:00:00',
   'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103026, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200251,103026,'学生预售票',150,150,180,180,6,0),(200252,103026,'全价预售票',220,220,600,600,6,0),
  (200253,103026,'现场全价票',280,280,240,240,6,0),(200254,103026,'VIP 门票 (含优先入场)',480,480,180,180,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103026,5),(103026,6);

-- 27: 赵雷 × 成都凤凰山 (演唱会)
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103027, '「赵雷」"我们的时光"2026 世界巡回演唱会 - 成都站', 1, 101005, '2026-12-27 19:30:00',
   'https://images.unsplash.com/photo-1487180142328-054b783fc471?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103027, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200261,103027,'看台 380',380,380,2700,2700,6,0),(200262,103027,'看台 580',580,580,4500,4500,6,0),
  (200263,103027,'看台 780',780,780,3600,3600,6,0),(200264,103027,'看台 980',980,980,2700,2700,6,0),
  (200265,103027,'内场 1380',1380,1380,2700,2700,6,0),(200266,103027,'内场 1680 (极速抢票)',1680,1680,1800,1800,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103027,17);

-- 28: 薛之谦 × 上海梅奔
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103028, '「薛之谦」"摩天大楼"2026 新年特别专场演唱会 - 上海站', 1, 101002, '2026-07-05 19:30:00',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103028, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200271,103028,'看台 380',380,380,2700,1800,6,0),(200272,103028,'看台 580',580,580,4500,3200,6,0),
  (200273,103028,'看台 780',780,780,3600,2500,6,0),(200274,103028,'看台 980',980,980,2700,1500,6,0),
  (200275,103028,'内场 1380',1380,1380,2700,800,6,0),(200276,103028,'内场 1680 (极速抢票)',1680,1680,1800,100,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103028,1),(103028,2);

-- 29: 重塑雕像的权利 × 上海 Modern Sky LAB
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103029, '「重塑雕像的权利」"噪音几何"2026 Livehouse 极致声学巡演 - 上海站', 0, 101007, '2026-07-11 20:00:00',
   'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103029, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200281,103029,'学生预售票',150,150,150,80,6,0),(200282,103029,'全价预售票',220,220,500,200,6,0),
  (200283,103029,'现场全价票',280,280,200,60,6,0),(200284,103029,'VIP 门票 (含优先入场)',480,480,150,20,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103029,5),(103029,6);

-- 30: 房东的猫 × 深圳 HOU
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103030, '「房东的猫」"关于告别与重逢的信"特别限定Live专场 - 深圳站', 0, 101006, '2026-07-25 20:00:00',
   'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103030, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200291,103030,'学生预售票',150,150,120,70,6,0),(200292,103030,'全价预售票',220,220,400,200,6,0),
  (200293,103030,'现场全价票',280,280,160,60,6,0),(200294,103030,'VIP 门票 (含优先入场)',480,480,120,25,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103030,17);

-- 31: GAI周延 × 北京 MAO Livehouse
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103031, '「GAI周延」"盖世英雄"特别限定Live专场 - 北京站', 0, 101008, '2026-08-01 20:00:00',
   'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103031, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200301,103031,'学生预售票',150,150,120,50,6,0),(200302,103031,'全价预售票',220,220,400,150,6,0),
  (200303,103031,'现场全价票',280,280,160,40,6,0),(200304,103031,'VIP 门票 (含优先入场)',480,480,120,10,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103031,9),(103031,10);

-- 32: 马思唯 × 上海 Modern Sky LAB
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103032, '「马思唯」"黑马"2026 Livehouse 极致声学巡演 - 上海站', 0, 101007, '2026-08-15 20:00:00',
   'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103032, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200311,103032,'学生预售票',150,150,150,150,6,0),(200312,103032,'全价预售票',220,220,500,500,6,0),
  (200313,103032,'现场全价票',280,280,200,200,6,0),(200314,103032,'VIP 门票 (含优先入场)',480,480,150,150,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103032,9),(103032,10);

-- 33: 五月天 × 成都凤凰山
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103033, '「五月天」"人生无限公司"2026 巡回双人间音乐盛典 - 成都站', 1, 101005, '2026-08-29 19:30:00',
   'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103033, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200321,103033,'看台 380',380,380,2700,2000,6,0),(200322,103033,'看台 580',580,580,4500,3500,6,0),
  (200323,103033,'看台 780',780,780,3600,2800,6,0),(200324,103033,'看台 980',980,980,2700,1800,6,0),
  (200325,103033,'内场 1380',1380,1380,2700,1000,6,0),(200326,103033,'内场 1680 (极速抢票)',1680,1680,1800,200,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103033,1),(103033,5);

-- 34: 告五人 × 广州太空间
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103034, '「告五人」"宇宙的有趣"2026 Livehouse 极致声学巡演 - 广州站', 0, 101010, '2026-09-12 20:00:00',
   'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=600', 2)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103034, 0, 0, 6, 1, 24, 0, 0.00, 1, 0) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 6;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200331,103034,'学生预售票',150,150,225,180,6,0),(200332,103034,'全价预售票',220,220,750,600,6,0),
  (200333,103034,'现场全价票',280,280,300,220,6,0),(200334,103034,'VIP 门票 (含优先入场)',480,480,225,150,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103034,1),(103034,6);

-- 35: 周杰伦 × 成都凤凰山
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`) VALUES
  (103035, '「周杰伦」"地表最强"2026 巡回双人间音乐盛典 - 成都站', 1, 101005, '2026-10-10 19:30:00',
   'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&q=80&w=600', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
INSERT INTO `t_event_config` VALUES (103035, 0, 1, 4, 2, 48, 24, 0.20, 1, 1) ON DUPLICATE KEY UPDATE `max_tickets_per_user` = 4;
INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`) VALUES
  (200341,103035,'看台 380',380,380,2700,2700,6,0),(200342,103035,'看台 580',580,580,4500,4500,6,0),
  (200343,103035,'看台 780',780,780,3600,3600,6,0),(200344,103035,'看台 980',980,980,2700,2700,6,0),
  (200345,103035,'内场 1380',1380,1380,2700,2700,6,0),(200346,103035,'内场 1680 (极速抢票)',1680,1680,1800,1800,6,0)
ON DUPLICATE KEY UPDATE `remaining_stock` = VALUES(`remaining_stock`);
INSERT IGNORE INTO `t_event_style_relation` VALUES (103035,1),(103035,2);

-- =============================================================================
-- 5. 用户基础测试数据
--    密码统一：BCrypt('LiveStart123') = $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- =============================================================================

-- ----- 手机号 → 用户 ID 全局映射 -----
INSERT INTO `t_user_phone_mapping` (`phone`, `user_id`) VALUES
  ('13800001001', 1001), ('13800001002', 1002), ('13800001003', 1003), ('13800001004', 1004),
  ('13800001005', 1005), ('13800001006', 1006), ('13800001007', 1007), ('13800001008', 1008),
  ('13800001009', 1009), ('13800001010', 1010), ('13800001011', 1011), ('13800001012', 1012),
  ('13900002001', 2001), ('13900002002', 2002), ('13900002003', 2003), ('13900002004', 2004),
  ('13700003001', 3001), ('13700003002', 3002), ('13700003003', 3003), ('13700003004', 3004)
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- =============================================================================
-- 6. 用户分库 ds_user_0 (偶数 user_id % 2 = 0)
-- =============================================================================
USE `ds_user_0`;

INSERT INTO `t_user_2` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1002, 'fan_zhang', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001002', 1, '张乐迷', 1, 1),
       (1010, 'fan_huang', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001010', 0, '黄乐迷', 1, 1),
       (2002, 'artist_wanqing', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13900002002', 1, '万青乐队', 2, 1),
       (3002, 'venue_admin_sh', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13700003002', 1, '上海梅奔场馆经理', 3, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`), `phone` = VALUES(`phone`);

INSERT INTO `t_user_4` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1004, 'fan_li', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001004', 1, '李乐迷', 1, 1),
       (1012, 'fan_zhao', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001012', 1, '赵乐迷', 1, 1),
       (2004, 'artist_zhangxueyou', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13900002004', 1, '张学友', 2, 1),
       (3004, 'host_modernsky', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13700003004', 1, '摩登天空主办方', 3, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`), `phone` = VALUES(`phone`);

INSERT INTO `t_user_6` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1006, 'fan_chen', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001006', 1, '陈乐迷', 1, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

INSERT INTO `t_user_0` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1008, 'fan_zhou', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001008', 1, '周乐迷', 1, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

INSERT INTO `t_user_profile_2` (`user_id`, `mail`, `gender`, `signature`)
VALUES (1002, 'fan_zhang@test.com', 1, '热爱演唱会'), (1010, 'fan_huang@test.com', 2, '看遍世界 livehouse'),
       (2002, 'wanqing@test.com', 0, '万能青年旅店官方'), (3002, 'venue_sh@test.com', 1, '场馆管理')
ON DUPLICATE KEY UPDATE `mail` = VALUES(`mail`);

INSERT INTO `t_user_profile_4` (`user_id`, `mail`, `gender`, `signature`)
VALUES (1004, 'fan_li@test.com', 1, '抢票达人'), (1012, 'fan_zhao@test.com', 2, '不见不散'),
       (2004, 'zxy@test.com', 1, '学友官方'), (3004, 'modernsky@test.com', 1, '摩登天空')
ON DUPLICATE KEY UPDATE `mail` = VALUES(`mail`);

INSERT INTO `t_user_visitor_2` (`id`, `user_id`, `real_name`, `card_type`, `card_no`, `card_no_hash`, `mobile`)
VALUES (11002, 1002, '张乐迷', 1, 'AES_ENC_330102199001020002', 'HASH_330102199001020002', '13800001002'),
       (11010, 1010, '黄乐迷', 1, 'AES_ENC_330102199001020010', 'HASH_330102199001020010', '13800001010')
ON DUPLICATE KEY UPDATE `real_name` = VALUES(`real_name`);

INSERT INTO `t_user_visitor_4` (`id`, `user_id`, `real_name`, `card_type`, `card_no`, `card_no_hash`, `mobile`)
VALUES (11004, 1004, '李乐迷', 1, 'AES_ENC_330102199001020004', 'HASH_330102199001020004', '13800001004')
ON DUPLICATE KEY UPDATE `real_name` = VALUES(`real_name`);

-- 广播 t_event 到 ds_user_0（仅新增演出 ID，旧 9001~9003 保留）
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`)
SELECT `id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status` FROM `live_start`.`t_event`
WHERE `id` BETWEEN 103001 AND 103035
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);

-- =============================================================================
-- 7. 用户分库 ds_user_1 (奇数 user_id % 2 = 1)
-- =============================================================================
USE `ds_user_1`;

INSERT INTO `t_user_1` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1001, 'fan_wang', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001001', 1, '王乐迷', 1, 1),
       (1009, 'fan_xu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001009', 1, '徐乐迷', 1, 1),
       (2001, 'artist_jay', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13900002001', 1, '周杰伦', 2, 1),
       (3001, 'venue_admin_bj', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13700003001', 1, '北京工体场馆经理', 3, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`), `phone` = VALUES(`phone`);

INSERT INTO `t_user_3` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1003, 'fan_liu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001003', 1, '刘乐迷', 1, 1),
       (1011, 'fan_sun', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001011', 0, '孙乐迷', 1, 1),
       (2003, 'artist_chongsu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13900002003', 1, '重塑雕像的权利', 2, 1),
       (3003, 'host_taihe', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13700003003', 1, '太合音乐主办方', 3, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`), `phone` = VALUES(`phone`);

INSERT INTO `t_user_5` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1005, 'fan_wu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001005', 1, '吴乐迷', 1, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

INSERT INTO `t_user_7` (`id`, `username`, `password`, `phone`, `is_verified`, `real_name`, `user_type`, `status`)
VALUES (1007, 'fan_zheng', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001007', 1, '郑乐迷', 1, 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

INSERT INTO `t_user_profile_1` (`user_id`, `mail`, `gender`, `signature`)
VALUES (1001, 'fan_wang@test.com', 1, '十年老粉'), (1009, 'fan_xu@test.com', 1, '日常逛 livehouse'),
       (2001, 'jay@test.com', 1, '周董官方'), (3001, 'venue_bj@test.com', 1, '工体')
ON DUPLICATE KEY UPDATE `mail` = VALUES(`mail`);

INSERT INTO `t_user_profile_3` (`user_id`, `mail`, `gender`, `signature`)
VALUES (1003, 'fan_liu@test.com', 2, '随时准备抢票'), (1011, 'fan_sun@test.com', 2, '坐前排'),
       (2003, 'chongsu@test.com', 0, '后朋克'), (3003, 'taihe@test.com', 1, '太合')
ON DUPLICATE KEY UPDATE `mail` = VALUES(`mail`);

INSERT INTO `t_user_visitor_1` (`id`, `user_id`, `real_name`, `card_type`, `card_no`, `card_no_hash`, `mobile`)
VALUES (11001, 1001, '王乐迷', 1, 'AES_ENC_330102199001020001', 'HASH_330102199001020001', '13800001001')
ON DUPLICATE KEY UPDATE `real_name` = VALUES(`real_name`);

INSERT INTO `t_user_visitor_3` (`id`, `user_id`, `real_name`, `card_type`, `card_no`, `card_no_hash`, `mobile`)
VALUES (11003, 1003, '刘乐迷', 1, 'AES_ENC_330102199001020003', 'HASH_330102199001020003', '13800001003')
ON DUPLICATE KEY UPDATE `real_name` = VALUES(`real_name`);

-- 广播 t_event 到 ds_user_1
INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`)
SELECT `id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status` FROM `live_start`.`t_event`
WHERE `id` BETWEEN 103001 AND 103035
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);

-- =============================================================================
-- 8. 订单分库广播 t_event，并写入测试订单
-- =============================================================================
USE `ds_order_0`;

INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`)
SELECT `id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status` FROM `live_start`.`t_event`
WHERE `id` BETWEEN 103001 AND 103035
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);

INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`)
SELECT `id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`
FROM `live_start`.`t_ticket_sku` WHERE `event_id` BETWEEN 103001 AND 103035
ON DUPLICATE KEY UPDATE `total_stock` = VALUES(`total_stock`), `remaining_stock` = VALUES(`remaining_stock`);

-- 测试订单 (user 1002 → t_order_10，已支付周杰伦看台 2 张)
INSERT INTO `t_order_10` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000002, 'TEST_ORDER_002', 1002, 1160.00, 1, '2026-06-15 10:01:00', '2026-06-15 10:00:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_10` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000002, 'TEST_ORDER_002', 1002, 11002, 103001, 200001, NULL, 'CHKCODE_002_A', 0),
       (6000003, 'TEST_ORDER_002', 1002, 11002, 103001, 200001, NULL, 'CHKCODE_002_B', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);
INSERT INTO `t_user_ticket_10` (`id`, `user_id`, `ticket_sku_id`, `event_id`, `status`, `check_code`)
VALUES (7000002, 1002, 200001, 103001, 0, 'CHKCODE_002_A'),
       (7000003, 1002, 200001, 103001, 0, 'CHKCODE_002_B')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

-- 测试订单 (user 1004 → t_order_12，待支付万青 Livehouse 学生票)
INSERT INTO `t_order_12` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000004, 'TEST_ORDER_004', 1004, 150.00, 0, NULL, '2026-06-21 14:30:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_12` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000004, 'TEST_ORDER_004', 1004, 11004, 103007, 200061, NULL, 'CHKCODE_004_A', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);

-- 测试订单 (user 1006 → t_order_14，已支付赵雷 Livehouse)
INSERT INTO `t_order_14` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000006, 'TEST_ORDER_006', 1006, 220.00, 1, '2026-06-10 09:15:00', '2026-06-10 09:14:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_14` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000006, 'TEST_ORDER_006', 1006, 11002, 103013, 200122, NULL, 'CHKCODE_006_A', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);
INSERT INTO `t_user_ticket_14` (`id`, `user_id`, `ticket_sku_id`, `event_id`, `status`, `check_code`)
VALUES (7000006, 1006, 200122, 103013, 0, 'CHKCODE_006_A')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

-- 测试订单 (user 1008 → t_order_8，已支付新裤子站票 2 张)
INSERT INTO `t_order_8` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000008, 'TEST_ORDER_008', 1008, 440.00, 1, '2026-06-18 11:45:00', '2026-06-18 11:44:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_8` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000008, 'TEST_ORDER_008', 1008, 11002, 103009, 200082, NULL, 'CHKCODE_008_A', 0),
       (6000009, 'TEST_ORDER_008', 1008, 11002, 103009, 200082, NULL, 'CHKCODE_008_B', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);
INSERT INTO `t_user_ticket_8` (`id`, `user_id`, `ticket_sku_id`, `event_id`, `status`, `check_code`)
VALUES (7000008, 1008, 200082, 103009, 0, 'CHKCODE_008_A'),
       (7000009, 1008, 200082, 103009, 0, 'CHKCODE_008_B')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

USE `ds_order_1`;

INSERT INTO `t_event` (`id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status`)
SELECT `id`, `title`, `event_type`, `venue_id`, `start_time`, `poster_url`, `status` FROM `live_start`.`t_event`
WHERE `id` BETWEEN 103001 AND 103035
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `status` = VALUES(`status`);

INSERT INTO `t_ticket_sku` (`id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`)
SELECT `id`,`event_id`,`title`,`original_price`,`selling_price`,`total_stock`,`remaining_stock`,`limit_num`,`version`
FROM `live_start`.`t_ticket_sku` WHERE `event_id` BETWEEN 103001 AND 103035
ON DUPLICATE KEY UPDATE `total_stock` = VALUES(`total_stock`), `remaining_stock` = VALUES(`remaining_stock`);

-- 测试订单 (user 1001 → t_order_9，已支付五月天看台)
INSERT INTO `t_order_9` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000001, 'TEST_ORDER_001', 1001, 580.00, 1, '2026-06-12 20:00:00', '2026-06-12 19:59:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_9` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000001, 'TEST_ORDER_001', 1001, 11001, 103006, 200052, NULL, 'CHKCODE_001_A', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);
INSERT INTO `t_user_ticket_9` (`id`, `user_id`, `ticket_sku_id`, `event_id`, `status`, `check_code`)
VALUES (7000001, 1001, 200052, 103006, 0, 'CHKCODE_001_A')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

-- 测试订单 (user 1003 → t_order_11，待支付 GAI 周延票)
INSERT INTO `t_order_11` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000003, 'TEST_ORDER_003', 1003, 220.00, 0, NULL, '2026-06-21 16:00:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_11` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000005, 'TEST_ORDER_003', 1003, 11003, 103015, 200142, NULL, 'CHKCODE_003_A', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);

-- 测试订单 (user 1005 → t_order_13，已取消 周杰伦内场)
INSERT INTO `t_order_13` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000005, 'TEST_ORDER_005', 1005, 1680.00, 3, NULL, '2026-06-05 13:20:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_13` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000007, 'TEST_ORDER_005', 1005, 11005, 103001, 200006, NULL, 'CHKCODE_005_A', 0)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);

-- 测试订单 (user 1007 → t_order_15，已核销 告五人 VIP)
INSERT INTO `t_order_15` (`id`, `order_no`, `user_id`, `total_amount`, `status`, `pay_time`, `create_time`)
VALUES (5000007, 'TEST_ORDER_007', 1007, 480.00, 2, '2026-05-20 10:00:00', '2026-05-20 09:59:00')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
INSERT INTO `t_order_item_15` (`id`, `order_no`, `user_id`, `visitor_id`, `event_id`, `sku_id`, `seat_id`, `check_code`, `is_checked`)
VALUES (6000011, 'TEST_ORDER_007', 1007, 11007, 103008, 200074, NULL, 'CHKCODE_007_A', 1)
ON DUPLICATE KEY UPDATE `is_checked` = VALUES(`is_checked`);
INSERT INTO `t_user_ticket_15` (`id`, `user_id`, `ticket_sku_id`, `event_id`, `status`, `check_code`)
VALUES (7000007, 1007, 200074, 103008, 1, 'CHKCODE_007_A')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 清理脚本（默认注释；执行前可取消注释整段单独执行）
-- =============================================================================
-- USE `live_start`;
-- DELETE FROM `t_event_style_relation` WHERE `event_id` BETWEEN 103001 AND 103035;
-- DELETE FROM `t_ticket_sku` WHERE `event_id` BETWEEN 103001 AND 103035;
-- DELETE FROM `t_event_config` WHERE `event_id` BETWEEN 103001 AND 103035;
-- DELETE FROM `t_event` WHERE `id` BETWEEN 103001 AND 103035;
-- DELETE FROM `t_performer_style_relation` WHERE `performer_id` BETWEEN 102001 AND 102016;
-- DELETE FROM `t_performer` WHERE `id` BETWEEN 102001 AND 102016;
-- DELETE FROM `t_venue` WHERE `id` BETWEEN 101001 AND 101010;
-- DELETE FROM `t_style` WHERE `id` BETWEEN 1 AND 17;
