package sa.factory.app1290148.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.factory.app1290148.model.LayoutSpecificationPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumLayoutProjectHomeScreen(
    viewModel: CurriculumLayoutProjectViewModel,
    onSelectItem: (LayoutSpecificationPage) -> Unit,
    onQuickCheckout: () -> Unit
) {
    val catalog by viewModel.catalog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("منصة خطوة لتنسيق ونشر المناهج التفاعلية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("تحويل مسودات كتب الأطفال ومناهج الت", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Bespoke Vector Graphic Header (>= 8 Paths branched on archetype)
                BespokeVectorIllustration(
                    archetype = "booking_timeline",
                    screenIdx = 1,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            }

            
            item {
                Text("اختر الموعد المتاح للحجز المباشر:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val days = listOf("السبت 14", "الأحد 15", "الاثنين 16", "الثلاثاء 17")
                    days.forEachIndexed { idx, day ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (idx == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(day.split(" ")[0], fontSize = 11.sp, color = if (idx == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(day.split(" ")[1], fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (idx == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Text("الفترات الزمنية المعتمدة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("09:00 ص", "11:30 ص", "02:00 م", "04:30 م").forEachIndexed { idx, slot ->
                        FilterChip(
                            selected = (idx == 1),
                            onClick = { },
                            label = { Text(slot, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("محددات الموعد والجدول الزمني المباشر:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• تأكيد فوري وحجز مباشر عبر المنظومة السحابية المعتمدة", fontSize = 12.sp, color = Color.Gray)
                        Text("• إشعار تقويم تلقائي SMS ورسالة تذكير قبل الموعد بساعتين", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onQuickCheckout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Text("حجز الموعد المختار وتأكيد الجلسة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        

            item {
                Text("قائمة الخدمات المعتمدة للتخصيص:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            items(catalog) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(item.description, color = Color.Gray, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onSelectItem(item) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("${item.priceMinorUnits / 100} SAR")
                        }
                    }
                }
            }
        }
    }
}
