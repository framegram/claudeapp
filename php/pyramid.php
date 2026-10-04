<?php
/**
 * 引数で指定した段数の「☆」ピラミッドを表示する
 *
 * @param int $rows 段数
 */
function pyramid($rows)
{
    for ($i = 1; $i <= $rows; $i++) {
        // 左側の空白（全角スペース）: 段数 - 現在の段
        for ($j = 0; $j < $rows - $i; $j++) {
            echo "　";
        }
        // 星の数: 2 × 現在の段 - 1
        for ($k = 0; $k < 2 * $i - 1; $k++) {
            echo "☆";
        }
        echo "<br>\n";
    }
}

echo "3段の時<br>\n";
pyramid(3);
echo "<br>\n";

echo "4段の時<br>\n";
pyramid(4);
echo "<br>\n";

echo "6段の時<br>\n";
pyramid(6);
