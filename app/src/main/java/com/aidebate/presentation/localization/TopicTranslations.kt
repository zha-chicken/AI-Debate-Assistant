package com.aidebate.presentation.localization

import com.aidebate.domain.model.DebateTopic

data class TopicTranslation(val title: String, val category: String, val description: String)

val topicTranslationsZh: Map<String, TopicTranslation> = mapOf(
    "AI should be regulated by governments" to TopicTranslation(
        "人工智能应受政府监管", "科技",
        "人工智能的发展和应用是否应受到政府的严格监督和监管？"
    ),
    "Social media does more harm than good" to TopicTranslation(
        "社交媒体弊大于利", "科技",
        "社交媒体对社会的负面影响是否超过了其带来的好处？"
    ),
    "Cryptocurrency is the future of finance" to TopicTranslation(
        "加密货币是金融的未来", "科技",
        "去中心化的数字货币会取代传统金融体系吗？"
    ),
    "Free will is an illusion" to TopicTranslation(
        "自由意志是幻觉", "哲学",
        "我们的选择是否真正自由，还是由先前原因所决定？"
    ),
    "Morality is subjective" to TopicTranslation(
        "道德是主观的", "哲学",
        "道德真理是客观存在的，还是取决于个人或文化视角？"
    ),
    "The ends justify the means" to TopicTranslation(
        "结果证明手段正当", "哲学",
        "好的结果能否在道德上证明为实现它们而采取的有害或不道德行为是正当的？"
    ),
    "We live in a simulation" to TopicTranslation(
        "我们生活在模拟中", "科学",
        "我们的现实是计算机模拟是否可能——甚至是很有可能？"
    ),
    "Genetic engineering of humans should be permitted" to TopicTranslation(
        "应允许人类基因工程", "科学",
        "是否应允许出于非医疗目的对人类胚胎进行基因改造？"
    ),
    "Space exploration is worth the cost" to TopicTranslation(
        "太空探索物有所值", "科学",
        "太空探索的科学和鼓舞人心的价值是否证明了其巨大开支的合理性？"
    ),
    "Universal basic income is necessary" to TopicTranslation(
        "全民基本收入是必要的", "政治",
        "政府是否应向所有公民提供保障性收入，无论其就业状况如何？"
    ),
    "Democracy is the best form of government" to TopicTranslation(
        "民主是最好的政府形式", "政治",
        "在所有背景下，民主是否真正优于其他治理形式？"
    ),
    "Privacy is more important than security" to TopicTranslation(
        "隐私比安全更重要", "政治",
        "个人隐私权是否应优先于集体安全措施？"
    ),
    "College education should be free" to TopicTranslation(
        "大学教育应免费", "教育",
        "高等教育是否应由公共资助，对所有学生免收学费？"
    ),
    "Standardized testing should be abolished" to TopicTranslation(
        "标准化考试应被废除", "教育",
        "标准化考试对教育的危害是否大于其衡量作用？"
    ),
    "Online learning is as effective as in-person" to TopicTranslation(
        "在线学习与线下学习同样有效", "教育",
        "虚拟教育能否达到或超过传统课堂学习的质量？"
    ),
    "AI-generated art is not real art" to TopicTranslation(
        "AI生成的艺术不是真正的艺术", "艺术",
        "艺术是否需要人类意图和情感才能被视为真正的艺术？"
    ),
    "Video games are a form of art" to TopicTranslation(
        "电子游戏是一种艺术形式", "艺术",
        "电子游戏是否应与电影、文学和视觉艺术一样获得认可？"
    ),
    "Censorship in art is never justified" to TopicTranslation(
        "艺术审查绝无正当理由", "艺术",
        "艺术表达是否应完全不受任何形式的审查？"
    ),
    "Remote work is better than office work" to TopicTranslation(
        "远程办公优于办公室办公", "科技",
        "在家工作是否比在办公室工作更高效、更令人满意？"
    ),
    "Nuclear energy is the best solution to climate change" to TopicTranslation(
        "核能是应对气候变化的最佳方案", "科学",
        "核能是否应成为减少碳排放的主要策略？"
    ),
)

fun DebateTopic.translate(languageCode: String?): DebateTopic {
    if (languageCode != "zh" || !isPredefined) return this
    val t = topicTranslationsZh[title] ?: return this
    return copy(title = t.title, category = t.category, description = t.description)
}
