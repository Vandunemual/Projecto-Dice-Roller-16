/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.diceroller

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diceroller.ui.theme.DiceRollerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiceRollerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiceRollerApp()
                }
            }
        }
    }
}

@Preview
@Composable
fun DiceRollerApp() {
    DiceWithButtonAndImage(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)
    )
}

/** Sorteia 6 números distintos entre 1 e 16. */
fun rollSix(): List<Int> = (1..16).shuffled().take(6)

@Composable
fun DiceWithButtonAndImage(modifier: Modifier = Modifier) {
    var numbers1 by remember { mutableStateOf(rollSix()) }
    var numbers2 by remember { mutableStateOf(rollSix()) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DipyramidDie(numbers = numbers1, modifier = Modifier.size(160.dp, 180.dp))
            DipyramidDie(numbers = numbers2, modifier = Modifier.size(160.dp, 180.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                numbers1 = rollSix()
                numbers2 = rollSix()
            },
        ) {
            Text(text = stringResource(R.string.roll), fontSize = 24.sp)
        }
    }
}

@Composable
fun DipyramidDie(numbers: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Ápices superior e inferior
        val top = Offset(cx, h * 0.04f)
        val bottom = Offset(cx, h * 0.96f)

        // 4 vértices do "equador" → formam 3 faces em cima e 3 em baixo
        val e0 = Offset(w * 0.06f, h * 0.42f)   // extremo esquerdo
        val e1 = Offset(w * 0.34f, h * 0.52f)   // centro-esquerda (mais baixo)
        val e2 = Offset(w * 0.66f, h * 0.52f)   // centro-direita (mais baixo)
        val e3 = Offset(w * 0.94f, h * 0.42f)   // extremo direito

        val edge = Color(0xFF7F0000)

        // Tons de vermelho: faces de cima mais claras, de baixo mais escuras
        val redTop = listOf(Color(0xFFE53935), Color(0xFFF44336), Color(0xFFE53935))
        val redBottom = listOf(Color(0xFFB71C1C), Color(0xFFC62828), Color(0xFFB71C1C))

        // Sombra no chão
        drawOval(
            color = Color(0x55000000),
            topLeft = Offset(w * 0.22f, h * 0.93f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.05f)
        )

        // Função auxiliar p/ desenhar uma face triangular
        fun face(a: Offset, b: Offset, c: Offset, fill: Color) {
            val path = Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
                lineTo(c.x, c.y)
                close()
            }
            drawPath(path, color = fill)
            drawPath(path, color = edge, style = Stroke(width = 3f))
        }

        // 3 faces superiores (ápice de cima)
        val topFaces = listOf(
            Triple(top, e0, e1),
            Triple(top, e1, e2),
            Triple(top, e2, e3),
        )
        // 3 faces inferiores (ápice de baixo)
        val bottomFaces = listOf(
            Triple(bottom, e0, e1),
            Triple(bottom, e1, e2),
            Triple(bottom, e2, e3),
        )

        topFaces.forEachIndexed { i, (a, b, c) -> face(a, b, c, redTop[i]) }
        bottomFaces.forEachIndexed { i, (a, b, c) -> face(a, b, c, redBottom[i]) }

        // Centroide de cada face (centro geométrico do triângulo)
        fun centroid(a: Offset, b: Offset, c: Offset) =
            Offset((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)

        val positions = (topFaces + bottomFaces).map { (a, b, c) -> centroid(a, b, c) }

        // Desenha os números centrados em cada face
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = h * 0.07f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(120, 0, 0, 0))
        }
        // Ajuste vertical p/ centrar o texto na sua posição
        val textOffset = (paint.descent() + paint.ascent()) / 2f

        numbers.forEachIndexed { i, n ->
            val p = positions[i]
            drawContext.canvas.nativeCanvas.drawText(
                n.toString(),
                p.x,
                p.y - textOffset,
                paint
            )
        }
    }
}
