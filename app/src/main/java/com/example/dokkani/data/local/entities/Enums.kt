package com.example.dokkani.data.local.entities

/**
 * أنواع الأطراف المتعاملة (عميل / مورد / كلاهما)
 */
enum class PartyType(val labelArabic: String) {
    CUSTOMER("عميل"),
    SUPPLIER("مورد"),
    BOTH("عميل ومورد")
}

/**
 * أنواع الفواتير في نظام دكاني
 */
enum class InvoiceType(val labelArabic: String) {
    SALE("فاتورة مبيعات"),
    PURCHASE("فاتورة مشتريات"),
    SALE_RETURN("مرتجع مبيعات"),
    PURCHASE_RETURN("مرتجع مشتريات")
}

/**
 * طرق الدفع والتحصيل المعتمدة في نظام دكاني
 */
enum class PaymentMethod(
    val labelArabic: String,
    val isPhysicalCash: Boolean = false,
    val requiresAccount: Boolean = false
) {
    CASH("نقداً (كاش)", isPhysicalCash = true),
    POS_CARD("شبكة / نقاط بيع (POS)", requiresAccount = true),
    MADA("شبكة مدى / بطاقة", requiresAccount = true),
    E_WALLET("محفظة إلكترونية", requiresAccount = true),
    BANK_TRANSFER("تحويل بنكي / شواخص", requiresAccount = true),
    EXCHANGE_NETWORK("شبكة صرافة / تحويلات", requiresAccount = true),
    CREDIT("آجل (على الحساب)"),
    MULTI("متعدد (كاش + شبكة/محفظة)");

    val isElectronic: Boolean
        get() = this == POS_CARD || this == MADA || this == E_WALLET || this == BANK_TRANSFER || this == EXCHANGE_NETWORK
}

/**
 * حالة الفاتورة
 */
enum class InvoiceStatus(val labelArabic: String) {
    COMPLETED("مكتملة"),
    DRAFT("مسودة"),
    CANCELLED("ملغاة")
}

/**
 * خيارات تقييم تكلفة المخزون المحاسبية لنظام دكاني
 */
enum class CostValuationMethod(val labelArabic: String, val descriptionArabic: String) {
    WAC("المتوسط المرجح (WAC)", "Weighted Average Cost - قسمة إجمالي تكلفة البضاعة المتاحة على إجمالي الكميات المتاحة."),
    FIFO("الوارد أولاً صادر أولاً (FIFO)", "First In, First Out - تقييم المخزون الصادر بناءً على أقدم طبقات الشراء المخزنة أولاً."),
    LIFO("الوارد أخيراً صادر أولاً (LIFO)", "Last In, First Out - تقييم المخزون الصادر بناءً على أحدث طبقات الشراء المخزنة أولاً."),
    LAST_PURCHASE_PRICE("آخر سعر شراء", "Last Purchase Price - اعتماد سعر آخر فاتورة توريد/شراء مسجلة للصنف.")
}

/**
 * أنواع حركات المخزون لتتبع الطبقات وحساب التكلفة
 */
enum class MovementType(val labelArabic: String) {
    PURCHASE_IN("شراء وتوريد للمخزن (+)"),
    SALE_OUT("بيع صرف من المخزن (-)"),
    RETURN_IN("مرتجع مبيعات وارد (+)"),
    RETURN_OUT("مرتجع مشتريات صادر (-)"),
    INVENTORY_ADJUSTMENT("تسوية جردية"),
    PRODUCE_SORTING("فرز خضار مشكل وارد (+)")
}

/**
 * حالة دفعة الخضار المشكل
 */
enum class BatchStatus(val labelArabic: String) {
    DRAFT("مسودة جرد"),
    SORTED("تم الفرز واحتساب الصافي"),
    STOCKED("تم توريدها للمخزن والرفوف")
}
