# Projeção Perspectiva: o que foi adicionado, por quê e como funciona

Este documento explica, desde o básico, as mudanças da branch `ajustes` em relação à `main`.
A principal é a **projeção perspectiva** da Aula 5, com a matriz do slide 25 e o ponto de fuga dos
slides 19 a 23. Também entraram algumas correções que deixaram o programa usável.

---

## Roteiro rápido (2 minutos para explicar ao professor)

1. O projeto já tinha três projeções **paralelas**: ortográfica (tecla `1`), Cavalier (`2`) e Cabinet (`3`).
2. Foi adicionada a **projeção perspectiva** (tecla `4`). Ela usa a matriz do slide 25, que faz
   `xp = d·x / (z+d)` e `yp = d·y / (z+d)`.
3. Na perspectiva, o que está mais longe fica **menor**, e as linhas de profundidade **convergem para
   um ponto de fuga**. No programa, esse ponto fica no **centro da janela**.
4. As teclas `R`/`F` aproximam ou afastam o **centro de projeção**, ou seja, mudam o `d`. Isso é o
   "mover o centro de projeção" do slide 26.
5. Pontos que ficam **atrás** do centro de projeção sairiam invertidos na tela. Por isso as arestas
   são **recortadas** antes da divisão.
6. Também foram corrigidos problemas que impediam o uso: o movimento WASD rápido demais, a janela
   maximizada que não atualizava, a abertura lenta e o foco do teclado.

---

## Parte 1: conceitos básicos

### 1.1 Coordenadas da tela

A tela é uma grade de pixels. No Java, a origem `(0,0)` fica no **canto superior esquerdo**:
`x` cresce para a direita e `y` cresce **para baixo**.

```
(0,0) ─────────────► x
  │
  │      tela
  │
  ▼
  y
```

### 1.2 Pontos 3D e profundidade

Um ponto 3D tem três coordenadas `(x, y, z)`. Neste projeto, `x` e `y` são como na tela, e `z` é a
**profundidade**: quanto maior o `z`, mais "para dentro" da tela, ou seja, mais longe de quem olha.

Os objetos (dois cubos e um carrinho) são feitos de **triângulos 3D**. O programa desenha só as arestas
de cada triângulo, em "aramado" (wireframe). Cada triângulo vira 3 linhas na tela.

### 1.3 O problema: a tela é 2D

A tela não tem profundidade. Para desenhar um ponto 3D, é preciso transformá-lo em um ponto 2D.
Isso se chama **projeção** (slide 8):

- existe um **plano de projeção**, que funciona como a tela. Aqui é o plano `z = 0`;
- de cada ponto do objeto sai uma linha, chamada **projetor**, até o **centro de projeção**, que é o olho ou a câmera;
- o ponto desenhado é onde o projetor **fura** o plano de projeção.

Há dois grandes tipos (slides 9 e 10):

| Tipo | Centro de projeção | Resultado |
|---|---|---|
| **Paralela** | no infinito, então os projetores são paralelos | mantém as proporções, mas não parece real (desenho técnico) |
| **Perspectiva** | a uma distância finita `d` | parece real (longe = menor), mas não mantém as proporções |

### 1.4 Matrizes 4×4 e coordenadas homogêneas

Toda transformação do programa (mover, girar, escalar, projetar) é feita multiplicando o ponto por
uma **matriz 4×4** (classe `Mat4x4`).

Um ponto 3D é escrito com **quatro** números, `(x, y, z, w)`, começando com `w = 1`. Isso se chama
**coordenadas homogêneas**. Depois da multiplicação, o ponto "de verdade" é obtido **dividindo tudo
por `w`**:

```
(x, y, z, w)  →  (x/w, y/w, z/w)
```

No código, essa divisão está no fim de `multiplicaPonto`
([Mat4x4.java](src/core3d/Mat4x4.java)):

```java
return new Ponto3D(x1/w1, y1/w1, z1/w1, w1/w1);
```

Por que isso importa? Uma matriz sozinha só sabe **somar e multiplicar**. A perspectiva precisa
**dividir pela profundidade**. O truque é colocar a profundidade em `w`, e a divisão por `w` faz o
resto. Essa é a ideia central da matriz de perspectiva (Parte 2).

### 1.5 O caminho de cada ponto até a tela

```
ponto do objeto ──► MODELVIEW ──► PROJEÇÃO ──► divide por w ──► drawLine na tela
                  (move, gira,   (3D → 2D)
                   escala)
```

- **modelview**: onde os objetos estão. As teclas `WASD`, `Q/E` e `Z/X` alteram essa matriz.
- **projeção**: como o 3D é "achatado" na tela. As teclas `1`, `2`, `3` e `4` trocam essa matriz.

Como as duas são separadas, dá para trocar a projeção sem mexer nos objetos.

### 1.6 As projeções que já existiam

| Tecla | Projeção | Fórmula | O tamanho muda com a distância? |
|---|---|---|---|
| `1` | Paralela ortográfica | `xp = x`, `yp = y` (o `z` é descartado) | não |
| `2` | Oblíqua Cavalier (α=1, θ=45°) | `xp = x + α·cos θ·z`, `yp = y + α·sin θ·z` | não |
| `3` | Oblíqua Cabinet (α=0,5, θ=30°) | mesma fórmula, com α e θ diferentes | não |

Em nenhuma delas há divisão: o `w` continua sempre 1. Por isso um objeto longe tem o mesmo
tamanho de um perto. A perspectiva é justamente o que muda isso.

---

## Parte 2: a projeção perspectiva (a funcionalidade nova)

### 2.1 A ideia (slide 19)

O centro de projeção fica a uma distância **finita** `d` do plano de projeção. Resultado:

- o tamanho do objeto projetado é **inversamente proporcional à distância**: quanto mais longe, menor;
- linhas paralelas que **não** são paralelas ao plano de projeção **convergem para um ponto de fuga**.

### 2.2 A fórmula, por semelhança de triângulos (slide 24)

Olhando "de lado", só com os eixos `x` e `z`: o centro de projeção (CP) está em `z = -d`, o plano de
projeção em `z = 0` e o ponto `P` em profundidade `z`.

```
  x
  ^
  |                                       * P = (x, z)
  |                                 ...   |
  |                            ...        |
  |                       ...             |
  |                   *                   | altura x
  |             ...   |                   |
  |        ...        | altura xp         |
  |   ...             |                   |
  *-------------------+-------------------+-----> z
  CP                  plano de            ponto P
  (z = -d)            projeção (z = 0)
  |<------- d ------->|<------- z ------->|
  |<-------------- z + d ---------------->|
```

O projetor sai do CP e vai até P. O triângulo pequeno (base `d`, altura `xp`) e o grande
(base `z + d`, altura `x`) são **semelhantes**, então:

```
xp / d = x / (z + d)     →     xp = d·x / (z + d)
yp / d = y / (z + d)     →     yp = d·y / (z + d)
zp = 0   (todo ponto projetado fica no plano z = 0)
```

**Exemplo com números** (`d = 500`, dois cantos de um cubo):

| Ponto | Conta | Resultado |
|---|---|---|
| `(100, 200, 0)`, na frente | `500·100/500`, `500·200/500` | `(100, 200)`: não muda, porque está no plano |
| `(100, 200, 100)`, atrás | `500·100/600`, `500·200/600` | `(83,3; 166,7)`: fica menor, mais perto da origem |

### 2.3 A matriz (slide 25) e como ela vira código

A matriz:

```
          | 1  0   0   0 |
M_per  =  | 0  1   0   0 |
          | 0  0   0   0 |
          | 0  0  1/d  1 |
```

Multiplicando pelo ponto `P = (x, y, z, 1)` (cada linha da matriz vezes a coluna do ponto):

```
           | 1  0   0   0 |   | x |     | x         |
M_per·P =  | 0  1   0   0 | · | y |  =  | y         |
           | 0  0   0   0 |   | z |     | 0         |
           | 0  0  1/d  1 |   | 1 |     | (z + d)/d |   ← este é o w
```

A última linha coloca `w = (z + d)/d`. Quando o programa divide por `w`, sai exatamente
`xp = d·x/(z+d)` e `yp = d·y/(z+d)`. **A matriz não divide nada: quem divide é o `w`.**

No código ([Mat4x4.java](src/core3d/Mat4x4.java)):

```java
public void setPerspectiveProjection(float d) {
    zera();

    mat[0][0] = 1;
    mat[1][1] = 1;

    mat[2][2] = 0;

    mat[3][2] = 1/d;
    mat[3][3] = 1;
}
```

É a matriz do slide, posição por posição (`mat[linha][coluna]`, contando a partir de 0).

### 2.4 Por que "centralizar" a perspectiva

Na matriz do slide, o centro de projeção fica alinhado com a **origem (0,0)**. Na tela, a origem é o
**canto superior esquerdo**. Com a matriz pura, tudo convergiria para esse canto, o que fica estranho.

A solução é combinar três matrizes, no método `setaPerspectiva()`
([MainCanvas.java](src/MainCanvas.java)):

1. **leva** o centro da janela `(cx, cy)` para a origem, com uma translação de `(-cx, -cy)`;
2. aplica a **matriz de perspectiva** do slide;
3. **traz de volta**, com uma translação de `(+cx, +cy)`.

```java
private void setaPerspectiva() {
    float cx = getWidth()/2;
    float cy = getHeight()/2;

    Mat4x4 paraOrigem = new Mat4x4();
    paraOrigem.setTranslate(-cx, -cy, 0);

    Mat4x4 per = new Mat4x4();
    per.setPerspectiveProjection(distanciaPerspectiva);

    Mat4x4 volta = new Mat4x4();
    volta.setTranslate(cx, cy, 0);

    projecao = projecao.multiplicaMatrizes(volta, projecao.multiplicaMatrizes(per, paraOrigem));
    nomeProjecao = "Perspectiva d="+(int)distanciaPerspectiva;
}
```

A fórmula final fica:

```
xp = cx + d·(x − cx) / (z + d)
yp = cy + d·(y − cy) / (z + d)
```

É a mesma fórmula do slide, só que medida a partir do centro da janela. É a mesma técnica de
"transladar para a origem, transformar e voltar" usada para girar em torno de um ponto.

Se a janela mudar de tamanho (por exemplo, ao maximizar), um `ComponentListener` chama
`setaPerspectiva()` de novo, para o centro acompanhar.

### 2.5 O ponto de fuga (slides 19, 21, 22 e 23)

**Existe ponto de fuga? Sim.** Ele não é uma variável do código: é uma **consequência** da matriz.

Pegue uma reta que se afasta na direção `(dx, dy, dz)`. Andando por ela até o infinito, a projeção
chega cada vez mais perto de:

```
( cx + d·dx/dz ,  cy + d·dy/dz )      (quando dz ≠ 0)
```

Esse é o **ponto de fuga** daquela família de retas paralelas.

- **Retas paralelas ao eixo Z** (direção `(0,0,1)`), como as arestas de profundidade dos cubos:
  o ponto de fuga é `(cx, cy)`, **o centro da janela**. Esta é a **perspectiva de um ponto de fuga**
  (slide 23, "one point perspective").
- **Retas paralelas a X ou Y** (`dz = 0`): não têm ponto de fuga e continuam paralelas. Por isso a
  face da frente de um cubo continua sendo um retângulo.
- **Girando a cena com `Q`/`E`** (rotação em torno de Y, ângulo θ), as arestas que eram de X passam a
  convergir para `(cx + d·cosθ/senθ, cy)` e as que eram de Z para `(cx − d·senθ/cosθ, cy)`. São
  **dois pontos de fuga** na mesma altura `y = cy`, que é a **linha do horizonte** (LH) do slide 21.
  Isso é a **perspectiva de dois pontos** do slide 23.
- **Três pontos de fuga** exigiriam girar também em torno do eixo X, e o programa não tem essa rotação.

Mover a cena com `WASD` **não** move o ponto de fuga. Ele depende só da **direção** das retas, não de
onde elas estão.

> Ao ativar a perspectiva (`4`), repare que as arestas de profundidade dos cubos apontam todas
> para o centro da janela.

### 2.6 O parâmetro `d` e as teclas R/F (slide 26)

`d` é a distância do centro de projeção (o olho) até o plano de projeção (a tela).

| `d` | Efeito |
|---|---|
| pequeno (ex.: 100) | perspectiva **forte**: o que está atrás encolhe muito, como uma lente grande-angular |
| grande (ex.: 2000) | perspectiva **suave** |
| → infinito | `d/(z+d)` → 1, então `xp → x`: vira a **projeção paralela ortográfica** |

A última linha liga as duas partes da matéria: **a projeção paralela é uma perspectiva com o centro
de projeção no infinito** (slide 9).

- `R` diminui `d` em 50 (mínimo 50) e aproxima o centro de projeção.
- `F` aumenta `d` em 50 e afasta o centro de projeção.

Isso corresponde ao **"Moving the Center of Projection"** do slide 26. O plano continua em `z = 0`,
então os pontos com `z = 0` **não mudam de lugar**: só o que está atrás muda.

### 2.7 Recorte de pontos atrás do centro de projeção

**O problema.** A divisão é por `w = (z + d)/d`:

- se `z > −d`, o ponto está **na frente** do olho: `w > 0`, e tudo certo;
- se `z = −d`, o ponto está **no olho**: `w = 0`, e há **divisão por zero**;
- se `z < −d`, o ponto está **atrás** do olho: `w < 0`, e a divisão **inverte o sinal**. O ponto aparece
  espelhado, do lado oposto da tela.

Isso acontece de verdade no programa: girando com `Q`/`E` ou aumentando com `X`, partes do carrinho
passam para trás do olho.

Exemplo (`d = 100`, centro em x=320): um ponto em `x = 420, z = −150` tem `w = −0,5`. Sem recorte, ele
seria desenhado em `x = 320 + 100·100/(−50) = 120`, **à esquerda** do centro, embora esteja à direita.

**A solução** (em [Triangulo3D.java](src/core3d/Triangulo3D.java)): antes de dividir, cada aresta é
verificada.

```java
static final float W_MINIMO = 0.1f;

private void desenhaAresta(Graphics2D dbg, Mat4x4 projection, Ponto3D a, Ponto3D b) {
    float wa = projection.calculaW(a);
    float wb = projection.calculaW(b);

    if (wa < W_MINIMO && wb < W_MINIMO) {
        return; // aresta inteira atras do centro de projecao
    }
    // recorta a parte da aresta que fica atras do centro de projecao
    if (wa < W_MINIMO) {
        a = interpola(a, b, (W_MINIMO - wa) / (wb - wa));
    } else if (wb < W_MINIMO) {
        b = interpola(b, a, (W_MINIMO - wb) / (wa - wb));
    }
    ...
}
```

- `calculaW(p)` (novo, em `Mat4x4`) calcula o `w` do ponto **antes** da divisão.
- Se as duas pontas estão atrás, a aresta não é desenhada.
- Se só uma está atrás, ela é **movida ao longo da aresta** até o ponto onde `w = 0,1`. Como `w` varia em
  linha reta ao longo da aresta, uma regra de três (`interpola`) encontra esse ponto exato.
- `0,1` em vez de `0` evita coordenadas gigantescas perto do olho. Na prática é um "plano de recorte
  próximo" em `z = −0,9·d`.

Nas projeções paralelas e oblíquas `w` é sempre 1, então o recorte nunca age e elas continuam iguais.

---

## Parte 3: correções que foram necessárias

Ao usar o programa de verdade, alguns problemas antigos, que já existiam na `main`, impediam de ver a
perspectiva funcionar.

### 3.1 WASD jogava a cena para fora da tela

**Problema.** O programa desenha cerca de **1000 quadros por segundo** (FPS). O movimento era de
**1 pixel por quadro**, ou seja, cerca de 1000 pixels por segundo. Um toque rápido em `D` fazia tudo
sumir da tela.

**Correção.** O movimento passou a depender do **tempo**, não do número de quadros:
`deslocamento = velocidade × tempo do quadro`. O código já tinha as variáveis `vel` e `difS` declaradas,
mas não usadas. Agora são usadas, com `vel = 200` pixels por segundo. Assim a velocidade é a mesma em
qualquer computador, rápido ou lento. De quebra, apertar duas teclas juntas (ex.: `W` + `A`) move na
diagonal. Antes, uma tecla anulava a outra.

### 3.2 Janela maximizada não atualizava

**Problema.** O laço de desenho atualizava só a região fixa `paintImmediately(0, 0, 640, 480)`, e o fundo
branco pintava só `800×600`. Com a janela maior, tudo fora dessa área ficava **congelado**, inclusive a
legenda com o nome da projeção.

**Correção.** Os dois passaram a usar o tamanho real do painel (`getWidth()`, `getHeight()`).

### 3.3 A janela demorava cerca de 15 segundos para abrir

**Problema.** O construtor imprimia no console os 64.000 bytes do arquivo `imgbmp.bmp`, uma linha por
byte. A janela só aparecia no fim.

**Correção.** Esse laço de depuração foi **comentado** (não apagado), e o arquivo agora é fechado após a
leitura. A janela abre em menos de 1 segundo.

### 3.4 Foco do teclado

**Problema.** Só o componente que tem o "foco" recebe as teclas. O painel era adicionado à janela
**depois** de ela aparecer, e clicar nele não devolvia o foco. Dependendo de como a janela era
aberta, as teclas não chegavam.

**Correção.** Em [MainClass.java](src/MainClass.java), o painel é adicionado **antes** de mostrar a janela
e pede o foco (`requestFocusInWindow()`). Além disso, **clicar no painel** sempre devolve o foco para ele.

---

## Parte 4: pequenas adições

- **Tecla `Espaço`**: volta a cena para a posição inicial (a `modelview` volta a ser a matriz
  identidade). Não muda a projeção escolhida.
- **Teclado numérico**: os números `1` a `4` do teclado numérico funcionam igual aos de cima.
- **Legenda na tela**: mostra a projeção atual (ex.: `Projecao: Perspectiva d=350`) e duas linhas de
  ajuda com as teclas.
- **`bin/.gitignore`**: inclui `/MainCanvas$4.class`, o arquivo compilado do novo listener de
  redimensionamento.

### Todas as teclas

| Tecla | Função |
|---|---|
| `1` | Projeção paralela ortográfica |
| `2` | Projeção oblíqua Cavalier |
| `3` | Projeção oblíqua Cabinet |
| **`4`** | **Projeção perspectiva** (nova) |
| **`R` / `F`** | **Diminui / aumenta `d`** na perspectiva (nova) |
| `W` `A` `S` `D` | Move a cena (agora por tempo) |
| `Q` / `E` | Gira a cena em torno do eixo Y |
| `Z` / `X` | Diminui / aumenta a escala |
| **`Espaço`** | **Reinicia a posição da cena** (nova) |
| Clique esquerdo ×3 | Cria um triângulo |
| Clique direito | Marca o ponto azul |

---

## Parte 5: arquivos alterados

| Arquivo | O que mudou |
|---|---|
| [src/core3d/Mat4x4.java](src/core3d/Mat4x4.java) | `setPerspectiveProjection(d)` (matriz do slide 25) e `calculaW(p)` |
| [src/core3d/Triangulo3D.java](src/core3d/Triangulo3D.java) | desenho aresta por aresta, com recorte (`desenhaAresta`, `interpola`) |
| [src/MainCanvas.java](src/MainCanvas.java) | teclas `4`/`R`/`F`/`Espaço`/numérico, `setaPerspectiva()`, listener de redimensionamento, movimento por tempo, redesenho da janela inteira, legenda e ajuda, laço de depuração comentado |
| [src/MainClass.java](src/MainClass.java) | painel adicionado antes de mostrar a janela, e foco no painel |
| `bin/.gitignore` | nova classe compilada ignorada |

---

## Parte 6: como foi testado

Os testes foram feitos fora do repositório (em uma pasta temporária), então não fazem parte do código
entregue:

1. **Conferência numérica da fórmula** (42 verificações). Entre elas:
   - a matriz dá exatamente `d·x/(z+d)` e `d·y/(z+d)` em vários pontos;
   - uma aresta a uma distância `z = d` fica com metade do tamanho;
   - pontos em `z = 0` não se movem;
   - uma reta ao longo de Z converge para o centro da janela (o ponto de fuga);
   - as teclas mudam `d` corretamente;
   - um vértice atrás do olho **não** aparece espelhado.
2. **Imagens** de cada projeção, conferidas visualmente.
3. **Uso simulado**: o programa foi aberto como um usuário faria, com cliques e teclas de verdade,
   janela maximizada e troca para outra janela. Cada passo foi conferido por captura de tela.

---

## Parte 7: perguntas que o professor pode fazer

**Por que a matriz é 4×4 se o espaço é 3D?**
Com a 4ª coordenada (`w`), translação vira multiplicação de matriz, e a divisão por `w` permite a
perspectiva. Com 3×3 não daria para fazer nenhuma das duas.

**Por que `zp = 0`?**
Todo ponto projetado cai no plano de projeção, que é `z = 0`. Por isso a 3ª linha da matriz é toda zero.

**Onde fica o ponto de fuga?**
No centro da janela, para as retas paralelas ao eixo Z. Ele aparece por causa da divisão por
`(z + d)`: quanto maior o `z`, mais o ponto se aproxima do centro (seção 2.5).

**É perspectiva de 1, 2 ou 3 pontos?**
De 1 ponto na posição inicial. Vira 2 pontos ao girar com `Q`/`E`. Não chega a 3, porque não há rotação
em torno do eixo X.

**Qual a diferença entre mover o centro de projeção e mover o plano (slide 26)?**
O programa move o **centro de projeção** (`R`/`F`) e mantém o plano fixo em `z = 0`. Isso muda **quanto**
as coisas de trás encolhem, mas o que está no plano não muda. Mover o **plano** com o centro fixo
só aumentaria ou diminuiria a imagem inteira por igual.

**O que acontece se `d` for muito grande?**
A perspectiva vai sumindo e o resultado se aproxima da projeção paralela ortográfica. A paralela é
o caso da perspectiva com o centro de projeção no infinito.

**Por que foi preciso recortar as arestas?**
Porque um ponto atrás do olho tem `w` negativo, e a divisão por `w` o desenharia espelhado (seção 2.7).

**Por que transladar para o centro antes de aplicar a matriz?**
A matriz do slide coloca o centro de projeção sobre a origem, que na tela é o canto superior esquerdo.
Transladar, aplicar e voltar põe o ponto de fuga no centro da janela (seção 2.4).

---

## Parte 8: limitações conhecidas

- O ponto de fuga **não é desenhado** na tela. Ele é percebido pela convergência das arestas.
- Não há rotação em torno do eixo X, então não é possível mostrar a perspectiva de 3 pontos.
- `Q`/`E` e `Z`/`X` giram e escalam em torno da **origem** (canto superior esquerdo, `z = 0`), não do
  centro dos objetos. Por isso a cena "balança" ao girar. Isso já era assim antes.
- **Bug antigo, não alterado**: em `criaCubo`, o último triângulo da face de cima é `(p6, p7, p8)`, mas
  deveria ser `(p7, p8, p4)`. Isso cria uma diagonal estranha no cubo, mais visível na perspectiva.
