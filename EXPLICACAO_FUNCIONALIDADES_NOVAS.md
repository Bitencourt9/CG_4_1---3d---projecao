# Funcionalidades novas: explicação do código

Este documento mostra **só o código das funcionalidades novas** da branch `ajustes`, ou seja, código que
**não existia** na `main`. Para cada trecho: primeiro o código, depois **o que faz**, **como funciona** e
**qual o objetivo**.

Ficaram de fora as **alterações em código que já existia**: movimento WASD por tempo, redesenho da
janela inteira, fundo do tamanho da janela, abertura rápida, foco do teclado, teclas `1`/`2`/`3` e
`bin/.gitignore`. Elas estão explicadas em [EXPLICACAO_CODIGO.md](EXPLICACAO_CODIGO.md), e os conceitos
por trás de tudo em [EXPLICACAO_PERSPECTIVA.md](EXPLICACAO_PERSPECTIVA.md).

## As 5 funcionalidades novas

| # | Funcionalidade | Como usar |
|---|---|---|
| A | [Projeção perspectiva](#a-projeção-perspectiva) | tecla `4`; `R`/`F` mudam o `d` |
| B | [Recorte de pontos atrás do centro de projeção](#b-recorte-de-pontos-atrás-do-centro-de-projeção) | automático, na perspectiva |
| C | [Reiniciar a cena](#c-reiniciar-a-cena) | tecla `Espaço` |
| D | [Teclado numérico](#d-teclado-numérico) | `1` a `4` do teclado numérico |
| E | [Legenda e ajuda na tela](#e-legenda-e-ajuda-na-tela) | sempre visível |

---

# A. Projeção perspectiva

A funcionalidade principal, da Aula 5 (slides 19 a 25). As peças se encaixam assim:

```
tecla 4 (perspectiva = true) ──┐
tecla R / F (muda o d) ────────┼──► setaPerspectiva() ──► projecao = translação × M_per × translação
janela muda de tamanho ────────┘
                                                              │
                                                              ▼
                                             usada para desenhar cada triângulo
```

## A1. `setPerspectiveProjection(d)`: a matriz de perspectiva

[Mat4x4.java:120-132](src/core3d/Mat4x4.java#L120-L132)

```java
// Matriz de perspectiva (Foley e Van Dam): centro de projecao em (0,0,-d),
// plano de projecao em z=0. Resulta em xp = d*x/(z+d), yp = d*y/(z+d), zp = 0
public void setPerspectiveProjection(float d) {
    zera();

    mat[0][0] = 1;
    mat[1][1] = 1;

    mat[2][2] = 0;

    mat[3][2] = 1/d;
    mat[3][3] = 1;
}
```

**O que faz:** transforma a matriz na **matriz de perspectiva do slide 25**:

```
| 1  0   0   0 |
| 0  1   0   0 |
| 0  0   0   0 |
| 0  0  1/d  1 |
```

**Como funciona:**
- `zera()` coloca 0 nas 16 posições. Depois, as posições do slide são preenchidas.
- `mat[linha][coluna]` conta a partir de 0. Então `mat[3][2]` é a **4ª linha, 3ª coluna**, onde o slide põe `1/d`.
- Multiplicando o ponto `(x, y, z, 1)` por essa matriz:
  - linha 0: `x`
  - linha 1: `y`
  - linha 2: `0`, porque todo ponto vai para o plano de projeção `z = 0`
  - linha 3: `z·(1/d) + 1 = (z + d)/d`, que é o **w**
- O método `multiplicaPonto`, que já existia, divide tudo por `w`. Sai exatamente `xp = d·x/(z+d)` e
  `yp = d·y/(z+d)`, as fórmulas do slide 24. **A matriz não divide nada: quem divide é o `w`.**
- `mat[2][2] = 0` já seria 0 por causa do `zera()`. Fica escrito para espelhar o slide.

**Objetivo:** ter a projeção perspectiva na classe de matrizes, ao lado das que já existiam
(`setParalelProjection` e `setObliqueProjection`). O `d` é a distância do centro de projeção (o olho) ao
plano de projeção (a tela). Quanto mais longe o ponto (maior o `z`), maior o `w` e menor ele fica na tela.

---

## A2. Estado da projeção

[MainCanvas.java:95-97](src/MainCanvas.java#L95-L97)

```java
String nomeProjecao = "Paralela";
boolean perspectiva = false;
float distanciaPerspectiva = 500; // d: distancia do centro de projecao ao plano z=0
```

**O que faz:** cria três variáveis que guardam a situação atual da projeção.

**Como funciona:**
- `nomeProjecao`: o texto mostrado na legenda (funcionalidade E). Começa como `"Paralela"`, a projeção
  com que o programa abre.
- `perspectiva`: `true` quando a perspectiva está ativa. As teclas `R`/`F` e o recálculo ao
  redimensionar só agem quando ela é `true`.
- `distanciaPerspectiva`: o `d` da fórmula. Começa em 500.
- As teclas `1`, `2` e `3` (código antigo) também atualizam `nomeProjecao` e colocam `perspectiva = false`.

**Objetivo:** a matriz `projecao` sozinha não diz qual projeção está ativa nem qual é o `d`. Com essas
variáveis, o programa consegue refazer a matriz quando o `d` ou a janela mudam, e mostrar a projeção na
tela.

---

## A3. Tecla `4`: liga a perspectiva

[MainCanvas.java:242-245](src/MainCanvas.java#L242-L245)

```java
if (key == KeyEvent.VK_4) {
    perspectiva = true;
    setaPerspectiva();
}
```

**O que faz:** ativa a projeção perspectiva.

**Como funciona:** fica dentro do `keyPressed`, que o Java chama a cada tecla apertada. Marca
`perspectiva = true` e chama `setaPerspectiva()` (A4), que monta a matriz e atualiza a legenda.

**Objetivo:** é a **entrada** da funcionalidade. As projeções antigas usam `1`, `2` e `3`, e a nova segue
a sequência.

---

## A4. `setaPerspectiva()`: monta a projeção centralizada

[MainCanvas.java:360-378](src/MainCanvas.java#L360-L378)

```java
// A matriz de perspectiva coloca o centro de projecao sobre a origem (0,0),
// que na tela e o canto superior esquerdo. Para o ponto de fuga ficar no meio
// da tela: leva o centro da tela para a origem, projeta e traz de volta.
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

**O que faz:** monta a matriz de projeção perspectiva **com o ponto de fuga no centro da janela** e
atualiza o texto da legenda.

**Como funciona:**
1. `cx, cy`: o centro do painel, metade da largura e metade da altura.
2. `paraOrigem`: translação que leva o centro da janela para a origem `(0,0)`.
3. `per`: a matriz de perspectiva do slide 25 (A1), com o `d` atual.
4. `volta`: translação que leva a origem de volta para o centro da janela.
5. As três são multiplicadas em uma só: `volta × (per × paraOrigem)`.
   - Aplicada a um ponto, ela age **da direita para a esquerda**: primeiro `paraOrigem`, depois `per`, depois `volta`.
   - O método `multiplicaMatrizes` (que já existia) usa só os dois parâmetros. Chamá-lo "a partir de" `projecao` é apenas a forma que a classe oferece.
6. O resultado vira a nova `projecao`, usada por todos os triângulos no próximo desenho.
7. A fórmula final é `xp = cx + d·(x − cx)/(z + d)`, e o mesmo vale para `y`.

**Objetivo:** a matriz do slide põe o centro de projeção sobre a origem, que na tela é o **canto superior
esquerdo**. Sem as translações, tudo convergiria para esse canto. Com elas, as arestas de profundidade
(paralelas ao eixo Z) convergem para o **centro da janela**. Esse é o **ponto de fuga** da perspectiva de
um ponto (slide 23).

---

## A5. Teclas `R` e `F`: mudam o `d`

[MainCanvas.java:246-254](src/MainCanvas.java#L246-L254)

```java
// R aproxima o centro de projecao (mais distorcao), F afasta
if (key == KeyEvent.VK_R && perspectiva) {
    distanciaPerspectiva = Math.max(50, distanciaPerspectiva-50);
    setaPerspectiva();
}
if (key == KeyEvent.VK_F && perspectiva) {
    distanciaPerspectiva += 50;
    setaPerspectiva();
}
```

**O que faz:** `R` diminui o `d` em 50 e `F` aumenta em 50. Os dois só funcionam com a perspectiva ativa.

**Como funciona:**
- `&& perspectiva` faz a tecla ser ignorada nas outras projeções.
- `Math.max(50, ...)` impede que `d` fique abaixo de 50. Com `d` perto de 0, a fórmula `d·x/(z+d)`
  deformaria tudo.
- Depois de mudar o `d`, chama `setaPerspectiva()` (A4) para refazer a matriz com o valor novo.
- Segurando a tecla, o sistema repete o evento, e o `d` continua mudando.

**Objetivo:** mostrar na prática o **"mover o centro de projeção"** do slide 26.
- **`d` pequeno:** perspectiva forte, e o que está atrás encolhe muito.
- **`d` grande:** perspectiva suave, cada vez mais parecida com a projeção paralela.

---

## A6. Listener de redimensionamento

[MainCanvas.java:6-7](src/MainCanvas.java#L6-L7) (imports) e
[MainCanvas.java:311-318](src/MainCanvas.java#L311-L318)

```java
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
```

```java
addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        if (perspectiva) {
            setaPerspectiva(); // mantem o ponto de fuga no centro da janela
        }
    }
});
```

**O que faz:** quando a janela muda de tamanho (maximizar, arrastar a borda) com a perspectiva ativa,
monta a matriz de novo.

**Como funciona:**
- Os dois `import` trazem as classes de eventos de componente, como a mudança de tamanho.
- `addComponentListener` registra um "ouvinte" que o Java chama quando o painel muda.
- `ComponentAdapter` é uma classe com todos os métodos vazios, e só `componentResized` foi sobrescrito.
  É o mesmo padrão dos `KeyListener` e `MouseListener` que o projeto já usa, mas sem precisar escrever
  os métodos que não são usados.
- Se a perspectiva estiver ativa, chama `setaPerspectiva()` (A4), que lê o tamanho **novo** do painel.

**Objetivo:** o ponto de fuga é calculado a partir do tamanho da janela. Sem este listener, ao maximizar,
ele ficaria no centro da janela **antiga**, e não no centro da tela.

---

# B. Recorte de pontos atrás do centro de projeção

Na perspectiva, a divisão é por `w = (z + d)/d`. Se um ponto fica **atrás do olho** (`z < −d`), `w` fica
negativo, e a divisão inverte o sinal: o ponto aparece **espelhado**, do lado oposto da tela. Se fica
exatamente no olho, `w = 0` e há divisão por zero. Isso acontece ao girar (`Q`/`E`) ou aumentar (`X`) a
cena. As quatro peças abaixo resolvem o problema.

## B1. `calculaW(p)`: o w antes da divisão

[Mat4x4.java:134-137](src/core3d/Mat4x4.java#L134-L137)

```java
// Coordenada homogenea w do ponto transformado, antes da divisao
public float calculaW(Ponto3D p) {
    return mat[3][0]*p.x +mat[3][1]*p.y +mat[3][2]*p.z +mat[3][3]*p.w;
}
```

**O que faz:** calcula só a coordenada `w` que o ponto teria depois de multiplicado pela matriz, **sem
fazer a divisão**.

**Como funciona:** é a mesma conta da última linha de `multiplicaPonto`: a linha 3 da matriz vezes o ponto.
Na perspectiva, o resultado é `(z + d)/d`. Nas projeções paralelas e oblíquas, é sempre 1.

**Objetivo:** o `multiplicaPonto` devolve o ponto **já dividido** por `w`, e o sinal de `w` se perde. Para
saber se um ponto está atrás do olho, é preciso olhar o `w` **antes** da divisão.

---

## B2. `W_MINIMO`: o limite do recorte

[Triangulo3D.java:33-35](src/core3d/Triangulo3D.java#L33-L35)

```java
// Menor w aceito antes da divisao. Na perspectiva, w = (z+d)/d, entao
// w <= 0 significa ponto atras do centro de projecao (sairia invertido na tela)
static final float W_MINIMO = 0.1f;
```

**O que faz:** define o menor `w` aceito para um ponto ser projetado.

**Como funciona:**
- `static final` significa que é uma **constante**, a mesma para todos os triângulos e que nunca muda.
- Com `w = (z + d)/d`, a condição `w ≥ 0,1` equivale a `z ≥ −0,9·d`. Na prática é um "plano de recorte
  próximo", um pouco à frente do olho.

**Objetivo:** usar 0,1 em vez de 0 evita a divisão por zero e também coordenadas gigantescas na tela
para pontos quase encostados no olho.

---

## B3. `desenhaAresta(...)`: recorte e desenho de uma aresta

[Triangulo3D.java:37-55](src/core3d/Triangulo3D.java#L37-L55)

```java
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

    Ponto3D a2 = projection.multiplicaPonto(a);
    Ponto3D b2 = projection.multiplicaPonto(b);

    dbg.drawLine((int)a2.x,(int)a2.y,(int)b2.x,(int)b2.y);
}
```

**O que faz:** desenha a aresta de `a` até `b`, mas só a parte que está **na frente** do olho. O método
`desenhase` do triângulo chama esta função para cada uma das 3 arestas.

**Como funciona:**
1. Calcula o `w` das duas pontas com `calculaW` (B1).
2. **As duas atrás** (`w < 0,1`): sai sem desenhar nada (`return`).
3. **Só `a` atrás:** troca `a` pelo ponto da aresta onde `w` vale exatamente 0,1.
   - Ao longo da aresta, `w` varia **em linha reta**: `w(t) = wa + t·(wb − wa)`, com `t` indo de 0 (em `a`) a 1 (em `b`).
   - Igualando a 0,1 sai `t = (0,1 − wa) / (wb − wa)`, o valor passado para `interpola` (B4).
   - Como `wa < 0,1 ≤ wb`, o denominador é positivo (sem divisão por zero) e `t` fica entre 0 e 1.
4. **Só `b` atrás:** faz o mesmo, com os papéis de `a` e `b` trocados.
5. **Nenhuma atrás:** não mexe em nada.
6. Por fim, projeta as duas pontas (com a divisão por `w`) e desenha a linha. O `(int)` converte para
   pixel, porque `drawLine` só aceita números inteiros.

**Objetivo:** impedir que pontos atrás do olho apareçam espelhados.

> Exemplo: com `d = 100`, um ponto em `x = 420, z = −150` tem `w = −0,5`. Sem recorte, ele seria
> desenhado em `x = 120`, do lado oposto ao real.

Nas projeções paralelas e oblíquas `w` é sempre 1, então nada é recortado e o desenho é igual ao de antes.

---

## B4. `interpola(...)`: ponto no meio da aresta

[Triangulo3D.java:57-62](src/core3d/Triangulo3D.java#L57-L62)

```java
private Ponto3D interpola(Ponto3D a, Ponto3D b, float t) {
    return new Ponto3D(a.x + (b.x - a.x)*t,
            a.y + (b.y - a.y)*t,
            a.z + (b.z - a.z)*t,
            a.w + (b.w - a.w)*t);
}
```

**O que faz:** devolve o ponto que fica a uma fração `t` do caminho entre `a` e `b`.

**Como funciona:** é a **interpolação linear**, uma "regra de três" em cada coordenada:
`início + (fim − início) × t`. Com `t = 0` o resultado é `a`, com `t = 1` é `b`, e com `t = 0,5` é o meio.
Cria um ponto **novo**, sem alterar `a` nem `b`.

**Objetivo:** achar o ponto exato onde a aresta cruza o limite `w = 0,1`, usado pelo recorte (B3).

---

# C. Reiniciar a cena

## C1. Tecla `Espaço`

[MainCanvas.java:220-222](src/MainCanvas.java#L220-L222)

```java
if (key == KeyEvent.VK_SPACE) {
    modelview.setIdentity(); // volta a cena para a posicao inicial
}
```

**O que faz:** ao apertar `Espaço`, a cena volta para a posição, rotação e escala iniciais.

**Como funciona:** a matriz `modelview` acumula todos os movimentos (`WASD`), giros (`Q`/`E`) e escalas
(`Z`/`X`). `setIdentity()` a transforma na **matriz identidade** (1 na diagonal e 0 no resto), que não
altera ponto nenhum. É como a `modelview` estava quando o programa abriu. A projeção escolhida e o `d`
**não** mudam.

**Objetivo:** se a cena sair da tela ou ficar deformada depois de muitos giros e escalas, dá para voltar ao
início sem fechar o programa.

---

# D. Teclado numérico

## D1. Números do teclado numérico

[MainCanvas.java:223-226](src/MainCanvas.java#L223-L226)

```java
// aceita tambem os numeros do teclado numerico
if (key >= KeyEvent.VK_NUMPAD1 && key <= KeyEvent.VK_NUMPAD4) {
    key = KeyEvent.VK_1 + (key - KeyEvent.VK_NUMPAD1);
}
```

**O que faz:** faz as teclas `1` a `4` do **teclado numérico** funcionarem igual às de cima das letras.

**Como funciona:**
- Para o Java, o `1` de cima (`VK_1`) e o `1` do teclado numérico (`VK_NUMPAD1`) são teclas **diferentes**,
  com códigos diferentes.
- Os códigos de cada grupo são sequenciais. Então `key - VK_NUMPAD1` dá 0, 1, 2 ou 3, e somando `VK_1`
  chega-se ao código equivalente de cima.
- A troca acontece **antes** dos `if` das teclas `1` a `4`, e eles passam a tratar os dois casos sem
  código duplicado.

**Objetivo:** quem usa o teclado numérico apertava `4` e nada acontecia.

---

# E. Legenda e ajuda na tela

## E1. Fonte da ajuda e textos no rodapé

[MainCanvas.java:53](src/MainCanvas.java#L53) (fonte) e
[MainCanvas.java:707-711](src/MainCanvas.java#L707-L711) (textos)

```java
Font fAjuda = new Font("", Font.PLAIN, 14);
```

```java
g.drawString("Projecao: "+nomeProjecao, 10, getHeight()-15);

g.setFont(fAjuda);
g.drawString("1 Paralela | 2 Cavalier | 3 Cabinet | 4 Perspectiva (R/F muda d)", 10, getHeight()-70);
g.drawString("WASD move | Q/E gira | Z/X escala | Espaco reinicia a cena", 10, getHeight()-52);
```

**O que faz:** escreve no rodapé da janela a projeção atual (ex.: `Projecao: Perspectiva d=350`) e,
acima dela, duas linhas com todas as teclas.

**Como funciona:**
- `fAjuda` é uma fonte de tamanho 14. O nome `""` usa a fonte padrão do sistema.
- Esses textos ficam no fim do método `paint`, que desenha a tela a cada quadro.
- `drawString(texto, x, y)` escreve o texto na posição dada. `y = getHeight() − 15` coloca a legenda
  sempre **15 pixels acima do fundo**, em qualquer tamanho de janela.
- A legenda usa a fonte grande (`f`, tamanho 30), que já estava ativa. Depois, `setFont(fAjuda)` troca
  para a pequena, para as duas linhas de ajuda caberem na janela.
- `nomeProjecao` (A2) é atualizado pelas teclas `1` a `4`, `R`/`F` e por `setaPerspectiva()`. Por isso a
  legenda sempre mostra a projeção certa e o `d` atual.
- A troca de fonte não atrapalha o próximo quadro, porque o `paint` começa com `g.setFont(f)`.

**Objetivo:** deixar visível **qual projeção está ativa** e qual o `d`, o que ajuda a comparar as projeções
durante a explicação, e mostrar as teclas para quem usa o programa pela primeira vez.
