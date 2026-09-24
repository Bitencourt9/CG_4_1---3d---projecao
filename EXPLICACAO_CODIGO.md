# Explicação do código: o que a branch `ajustes` adiciona em relação à `main`

Este documento mostra **cada trecho de código** que existe na branch `ajustes` e não existe na `main`.
Para cada um: primeiro o código, depois **o que faz**, **como funciona** e **qual o objetivo**.

Quando o trecho **altera** um código que já existia, aparecem as duas versões: **Antes (main)** e
**Agora (ajustes)**.

Para os conceitos por trás (matrizes, `w`, ponto de fuga), veja o
[EXPLICACAO_PERSPECTIVA.md](EXPLICACAO_PERSPECTIVA.md).

## Índice

**[Mat4x4.java](src/core3d/Mat4x4.java)**: a matemática

1. [`setPerspectiveProjection(d)`: a matriz de perspectiva](#1-setperspectiveprojectiond-a-matriz-de-perspectiva)
2. [`calculaW(p)`: o w antes da divisão](#2-calculawp-o-w-antes-da-divisão)

**[Triangulo3D.java](src/core3d/Triangulo3D.java)**: o desenho com recorte

3. [`desenhase(...)`: desenhar aresta por aresta](#3-desenhase-desenhar-aresta-por-aresta)
4. [`W_MINIMO`: o limite do recorte](#4-w_minimo-o-limite-do-recorte)
5. [`desenhaAresta(...)`: recorte e desenho de uma aresta](#5-desenhaaresta-recorte-e-desenho-de-uma-aresta)
6. [`interpola(...)`: ponto no meio da aresta](#6-interpola-ponto-no-meio-da-aresta)

**[MainCanvas.java](src/MainCanvas.java)**: a tela, as teclas e o laço do programa

7. [Novos imports](#7-novos-imports)
8. [Fonte da ajuda (`fAjuda`)](#8-fonte-da-ajuda-fajuda)
9. [Estado da projeção (`nomeProjecao`, `perspectiva`, `distanciaPerspectiva`)](#9-estado-da-projeção)
10. [Laço de depuração comentado e `fin.close()`](#10-laço-de-depuração-comentado-e-finclose)
11. [Tecla `Espaço`: reinicia a cena](#11-tecla-espaço-reinicia-a-cena)
12. [Teclado numérico](#12-teclado-numérico)
13. [Teclas `1`, `2`, `3` atualizando o estado](#13-teclas-1-2-3-atualizando-o-estado)
14. [Tecla `4`: liga a perspectiva](#14-tecla-4-liga-a-perspectiva)
15. [Teclas `R` e `F`: mudam o `d`](#15-teclas-r-e-f-mudam-o-d)
16. [Clique devolve o foco do teclado](#16-clique-devolve-o-foco-do-teclado)
17. [Listener de redimensionamento](#17-listener-de-redimensionamento)
18. [`setaPerspectiva()`: monta a projeção centralizada](#18-setaperspectiva-monta-a-projeção-centralizada)
19. [`simulaMundo(...)`: movimento por tempo](#19-simulamundo-movimento-por-tempo)
20. [`paint(...)`: fundo do tamanho da janela](#20-paint-fundo-do-tamanho-da-janela)
21. [`paint(...)`: legenda e ajuda na tela](#21-paint-legenda-e-ajuda-na-tela)
22. [`run()`: redesenhar a janela inteira](#22-run-redesenhar-a-janela-inteira)

**[MainClass.java](src/MainClass.java)**: a janela

23. [Painel antes de mostrar a janela, e foco](#23-painel-antes-de-mostrar-a-janela-e-foco)

**[bin/.gitignore](bin/.gitignore)**

24. [Ignorar `MainCanvas$4.class`](#24-ignorar-maincanvas4class)

---

# Mat4x4.java

## 1. `setPerspectiveProjection(d)`: a matriz de perspectiva

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
- `zera()` coloca 0 em todas as 16 posições. Depois, só as posições diferentes de zero são preenchidas.
- `mat[linha][coluna]` conta a partir de 0. Então `mat[3][2]` é a **4ª linha, 3ª coluna**, onde o slide põe `1/d`.
- Multiplicando o ponto `(x, y, z, 1)` por essa matriz:
  - linha 0: `x`
  - linha 1: `y`
  - linha 2: `0`, porque todo ponto vai para o plano `z = 0`
  - linha 3: `z·(1/d) + 1 = (z + d)/d`, que é o **w**
- O método `multiplicaPonto` (que já existia) divide tudo por `w`. O resultado é `xp = d·x/(z+d)` e
  `yp = d·y/(z+d)`, as fórmulas do slide 24.
- `mat[2][2] = 0` já seria 0 por causa do `zera()`. Fica escrito explicitamente para espelhar o slide.

**Objetivo:** ter a projeção perspectiva disponível na classe de matrizes, do mesmo jeito que já existiam
`setParalelProjection()` e `setObliqueProjection(...)`. O parâmetro `d` é a distância do centro de
projeção (o olho) até o plano de projeção (a tela).

---

## 2. `calculaW(p)`: o w antes da divisão

[Mat4x4.java:134-137](src/core3d/Mat4x4.java#L134-L137)

```java
// Coordenada homogenea w do ponto transformado, antes da divisao
public float calculaW(Ponto3D p) {
    return mat[3][0]*p.x +mat[3][1]*p.y +mat[3][2]*p.z +mat[3][3]*p.w;
}
```

**O que faz:** calcula só a coordenada `w` que o ponto teria depois de multiplicado pela matriz, **sem
fazer a divisão**.

**Como funciona:** é a mesma conta da última linha de `multiplicaPonto`: a linha 3 da matriz
multiplicada pelo ponto. Na perspectiva, o resultado é `(z + d)/d`.

**Objetivo:** o método `multiplicaPonto` já devolve o ponto **dividido** por `w`, e com isso o sinal de
`w` se perde. Para saber se um ponto está **atrás do olho** (`w` ≤ 0), é preciso olhar o `w` antes da
divisão. É isso que o recorte (item 5) usa.

---

# Triangulo3D.java

## 3. `desenhase(...)`: desenhar aresta por aresta

[Triangulo3D.java:23-31](src/core3d/Triangulo3D.java#L23-L31)

**Antes (main):**

```java
public void desenhase(Graphics2D dbg, Mat4x4 modelview,Mat4x4 projection) {
    Ponto3D pa1 = modelview.multiplicaPonto(pa);
    Ponto3D pb1 = modelview.multiplicaPonto(pb);
    Ponto3D pc1 = modelview.multiplicaPonto(pc);

    Ponto3D pa2 = projection.multiplicaPonto(pa1);
    Ponto3D pb2 = projection.multiplicaPonto(pb1);
    Ponto3D pc2 = projection.multiplicaPonto(pc1);

    dbg.drawLine((int)pa2.x,(int)pa2.y,(int)pb2.x,(int)pb2.y);
    dbg.drawLine((int)pb2.x,(int)pb2.y,(int)pc2.x,(int)pc2.y);
    dbg.drawLine((int)pc2.x,(int)pc2.y,(int)pa2.x,(int)pa2.y);
}
```

**Agora (ajustes):**

```java
public void desenhase(Graphics2D dbg, Mat4x4 modelview,Mat4x4 projection) {
    Ponto3D pa1 = modelview.multiplicaPonto(pa);
    Ponto3D pb1 = modelview.multiplicaPonto(pb);
    Ponto3D pc1 = modelview.multiplicaPonto(pc);

    desenhaAresta(dbg, projection, pa1, pb1);
    desenhaAresta(dbg, projection, pb1, pc1);
    desenhaAresta(dbg, projection, pc1, pa1);
}
```

**O que faz:** desenha as 3 arestas do triângulo (a→b, b→c, c→a).

**Como funciona:**
- A primeira parte não mudou: os três vértices passam pela **modelview** (posição, rotação e escala da cena).
- Antes, os três vértices eram projetados e ligados direto com `drawLine`.
- Agora, cada aresta vai para `desenhaAresta` (item 5), que **decide se e como** desenhar antes de projetar.

**Objetivo:** permitir o **recorte**. Na perspectiva, um vértice pode ficar atrás do olho, e projetá-lo
direto o desenharia espelhado. O recorte precisa ser feito **por aresta**: ao cortar, surge um ponto novo
no meio dela.

---

## 4. `W_MINIMO`: o limite do recorte

[Triangulo3D.java:33-35](src/core3d/Triangulo3D.java#L33-L35)

```java
// Menor w aceito antes da divisao. Na perspectiva, w = (z+d)/d, entao
// w <= 0 significa ponto atras do centro de projecao (sairia invertido na tela)
static final float W_MINIMO = 0.1f;
```

**O que faz:** define o menor valor de `w` aceito para um ponto ser projetado.

**Como funciona:**
- `static final` significa que é uma **constante**, a mesma para todos os triângulos e que não muda.
- Com `w = (z + d)/d`, a condição `w ≥ 0,1` equivale a `z ≥ −0,9·d`. Na prática é um "plano de recorte
  próximo", um pouco à frente do olho.

**Objetivo:** evitar dois problemas:
- `w = 0` causa **divisão por zero**;
- `w` muito perto de 0 gera coordenadas **gigantescas** na tela.

Usar 0,1 em vez de 0 dá uma margem de segurança.

---

## 5. `desenhaAresta(...)`: recorte e desenho de uma aresta

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

**O que faz:** desenha a aresta de `a` até `b`, mas só a parte que está **na frente** do olho.

**Como funciona:**
1. Calcula o `w` das duas pontas com `calculaW` (item 2).
2. **As duas atrás** (`w < 0,1`): sai do método sem desenhar nada (`return`).
3. **Só `a` atrás:** troca `a` pelo ponto da aresta onde `w` vale exatamente 0,1.
   - Ao longo da aresta, `w` varia **em linha reta**: `w(t) = wa + t·(wb − wa)`, com `t` indo de 0 (em `a`) a 1 (em `b`).
   - Igualando a 0,1 sai `t = (0,1 − wa) / (wb − wa)`, que é o valor passado para `interpola` (item 6).
   - Como `wa < 0,1 ≤ wb`, o denominador é positivo (sem divisão por zero) e `t` fica entre 0 e 1.
4. **Só `b` atrás:** faz o mesmo, trocando os papéis de `a` e `b`.
5. **Nenhuma atrás:** não mexe em nada.
6. Por fim, projeta as duas pontas (com a divisão por `w`) e desenha a linha. O `(int)` converte para
   pixel, porque `drawLine` só aceita números inteiros.

**Objetivo:** impedir que pontos atrás do olho apareçam **espelhados** na tela.

> Exemplo: com `d = 100`, um ponto em `x = 420, z = −150` tem `w = −0,5`. Sem recorte, ele seria
> desenhado em `x = 120`, do lado oposto ao real.

Nas projeções paralelas e oblíquas `w` é sempre 1, então nada é recortado e o resultado é igual ao de antes.

---

## 6. `interpola(...)`: ponto no meio da aresta

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
Cria um **novo** ponto, sem alterar `a` nem `b`.

**Objetivo:** achar o ponto exato onde a aresta cruza o limite `w = 0,1`, usado pelo recorte (item 5).

---

# MainCanvas.java

## 7. Novos imports

[MainCanvas.java:6-7](src/MainCanvas.java#L6-L7)

```java
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
```

**O que faz:** traz para o arquivo duas classes do Java que tratam eventos de **componentes**, como
mudar de tamanho.

**Como funciona:** o `import` só permite escrever `ComponentAdapter` em vez do nome completo
`java.awt.event.ComponentAdapter`.

**Objetivo:** usar essas classes no listener de redimensionamento (item 17).

---

## 8. Fonte da ajuda (`fAjuda`)

[MainCanvas.java:53](src/MainCanvas.java#L53)

```java
Font fAjuda = new Font("", Font.PLAIN, 14);
```

**O que faz:** cria uma fonte simples de tamanho 14.

**Como funciona:** o nome `""` usa a fonte padrão do sistema, igual à fonte `f` (tamanho 30) que já existia.

**Objetivo:** escrever as linhas de ajuda com as teclas (item 21) em letra menor, para caberem na janela.

---

## 9. Estado da projeção

[MainCanvas.java:95-97](src/MainCanvas.java#L95-L97)

```java
String nomeProjecao = "Paralela";
boolean perspectiva = false;
float distanciaPerspectiva = 500; // d: distancia do centro de projecao ao plano z=0
```

**O que faz:** cria três variáveis que guardam a situação atual da projeção.

**Como funciona:**
- `nomeProjecao`: o texto mostrado na legenda. Começa como `"Paralela"`, porque o programa inicia com a projeção paralela.
- `perspectiva`: `true` quando a perspectiva está ativa. Serve para as teclas `R`/`F` e o
  redimensionamento só agirem nesse modo.
- `distanciaPerspectiva`: o `d` da fórmula. Começa em 500 e muda com `R`/`F`.

**Objetivo:** a matriz `projecao` sozinha não diz qual projeção está ativa nem qual é o `d`. Guardar isso
permite mostrar na tela, recalcular a matriz quando `d` ou o tamanho da janela mudam, e ligar ou
desligar as teclas certas.

---

## 10. Laço de depuração comentado e `fin.close()`

[MainCanvas.java:107-112](src/MainCanvas.java#L107-L112)

**Antes (main):**

```java
System.out.println("Bytes Lidos "+byteslidos);
for(int i = 0; i < byteslidos;i++) {
    System.out.println(i+": "+todosodbytes[i]);
}
```

**Agora (ajustes):**

```java
System.out.println("Bytes Lidos "+byteslidos);
// imprimir os 64000 bytes atrasava a abertura da janela em ~15s
//for(int i = 0; i < byteslidos;i++) {
//	System.out.println(i+": "+todosodbytes[i]);
//}
fin.close();
```

**O que faz:** para de imprimir cada byte do arquivo `imgbmp.bmp` no console e fecha o arquivo depois
de lê-lo.

**Como funciona:**
- O construtor lê até 64.000 bytes do `imgbmp.bmp`. O laço antigo imprimia **uma linha por byte**, ou seja,
  64.000 linhas, antes de a janela aparecer. O laço foi **comentado** (com `//`), e não apagado, para
  poder ser reativado se necessário. A linha "Bytes Lidos" continua sendo impressa.
- `fin.close()` libera o arquivo, que antes ficava aberto até o programa terminar.

**Objetivo:** a janela demorava cerca de **15 segundos** para abrir, e agora abre em menos de 1 segundo.

---

## 11. Tecla `Espaço`: reinicia a cena

[MainCanvas.java:220-222](src/MainCanvas.java#L220-L222)

```java
if (key == KeyEvent.VK_SPACE) {
    modelview.setIdentity(); // volta a cena para a posicao inicial
}
```

**O que faz:** ao apertar `Espaço`, a cena volta para a posição, rotação e escala iniciais.

**Como funciona:** a `modelview` acumula todos os movimentos (`WASD`), giros (`Q`/`E`) e escalas (`Z`/`X`).
`setIdentity()` a transforma na **matriz identidade**, que não muda nada no ponto. É como a
`modelview` estava quando o programa abriu. A projeção escolhida e o `d` **não** mudam.

**Objetivo:** se a cena sair da tela ou ficar distorcida, dá para voltar ao início sem fechar o programa.

---

## 12. Teclado numérico

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
- Os códigos de cada grupo são sequenciais (`VK_NUMPAD1`, `VK_NUMPAD2`, ...). Então
  `key - VK_NUMPAD1` dá 0, 1, 2 ou 3, e somando `VK_1` chega-se ao código equivalente de cima.
- A variável `key` é trocada **antes** dos `if` das teclas `1` a `4`, e eles tratam os dois casos sem
  precisar de código duplicado.

**Objetivo:** quem usasse o teclado numérico apertava `4` e nada acontecia.

---

## 13. Teclas `1`, `2`, `3` atualizando o estado

[MainCanvas.java:227-241](src/MainCanvas.java#L227-L241)

**Antes (main):**

```java
if (key == KeyEvent.VK_1) {
    projecao.setParalelProjection();
}
if (key == KeyEvent.VK_2) {
    projecao.setObliqueProjection(1, 45);
}
if (key == KeyEvent.VK_3) {
    projecao.setObliqueProjection(0.5f, 30);
}
```

**Agora (ajustes):**

```java
if (key == KeyEvent.VK_1) {
    projecao.setParalelProjection();
    nomeProjecao = "Paralela";
    perspectiva = false;
}
if (key == KeyEvent.VK_2) {
    projecao.setObliqueProjection(1, 45);
    nomeProjecao = "Cavalier";
    perspectiva = false;
}
if (key == KeyEvent.VK_3) {
    projecao.setObliqueProjection(0.5f, 30);
    nomeProjecao = "Cabinet";
    perspectiva = false;
}
```

**O que faz:** continua trocando a projeção, e agora também atualiza o nome mostrado na tela e marca
que a perspectiva **não** está ativa.

**Como funciona:** as duas linhas novas em cada `if` só atualizam as variáveis do item 9.

**Objetivo:**
- a legenda passa a mostrar a projeção certa;
- ao sair da perspectiva, `perspectiva = false` desliga as teclas `R`/`F` e o recálculo no redimensionamento, que só fazem sentido nela.

---

## 14. Tecla `4`: liga a perspectiva

[MainCanvas.java:242-245](src/MainCanvas.java#L242-L245)

```java
if (key == KeyEvent.VK_4) {
    perspectiva = true;
    setaPerspectiva();
}
```

**O que faz:** ativa a projeção perspectiva.

**Como funciona:** marca `perspectiva = true` e chama `setaPerspectiva()` (item 18), que monta a matriz
de perspectiva centralizada na janela e atualiza o nome da legenda.

**Objetivo:** é a **entrada da funcionalidade nova**. As projeções antigas usam `1`, `2` e `3`, e a nova
segue a sequência.

---

## 15. Teclas `R` e `F`: mudam o `d`

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
- Depois de mudar o `d`, chama `setaPerspectiva()` para refazer a matriz com o novo valor.
- Segurando a tecla, o sistema repete o evento, e o `d` continua mudando.

**Objetivo:** mostrar na prática o efeito de **mover o centro de projeção** (slide 26).
- **`d` pequeno:** perspectiva forte.
- **`d` grande:** perspectiva suave, que se aproxima da projeção paralela.

---

## 16. Clique devolve o foco do teclado

[MainCanvas.java:268-269](src/MainCanvas.java#L268-L269)

**Antes (main):**

```java
public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub
    clickX = e.getX();
```

**Agora (ajustes):**

```java
public void mousePressed(MouseEvent e) {
    requestFocusInWindow(); // clicar no painel devolve o foco do teclado para ele
    clickX = e.getX();
```

**O que faz:** sempre que o usuário clica no painel, ele volta a receber as teclas.

**Como funciona:** só o componente que tem o **foco** recebe os eventos de teclado.
`requestFocusInWindow()` pede o foco para o painel. O comentário `TODO` automático do Eclipse foi
trocado por essa linha. O resto do método, que cria os triângulos com o clique, não mudou.

**Objetivo:** se o foco se perdesse (por exemplo, ao alternar entre janelas), as teclas paravam de
funcionar. Agora basta clicar no painel.

---

## 17. Listener de redimensionamento

[MainCanvas.java:311-318](src/MainCanvas.java#L311-L318)

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

**O que faz:** quando a janela muda de tamanho (maximizar, arrastar a borda) e a perspectiva está
ativa, recalcula a matriz de perspectiva.

**Como funciona:**
- `addComponentListener` registra um "ouvinte" que o Java chama quando o painel muda.
- `ComponentAdapter` é uma classe com métodos vazios, e só `componentResized` foi sobrescrito. É o mesmo
  padrão dos `KeyListener` e `MouseListener` que já existiam, mas sem precisar escrever os métodos que
  não são usados.
- É uma classe anônima (sem nome). O Java compila cada classe anônima em um arquivo `.class` próprio, e isso explica o item 24.

**Objetivo:** o centro da perspectiva (o ponto de fuga) é calculado a partir do **tamanho da janela**.
Sem este listener, ao maximizar, o ponto de fuga ficaria no centro da janela **antiga**.

---

## 18. `setaPerspectiva()`: monta a projeção centralizada

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
atualiza a legenda.

**Como funciona:**
1. `cx, cy`: o centro do painel, metade da largura e metade da altura.
2. `paraOrigem`: translação que leva o centro da janela para a origem `(0,0)`.
3. `per`: a matriz de perspectiva do slide 25 (item 1), com o `d` atual.
4. `volta`: translação que leva a origem de volta para o centro da janela.
5. As três são multiplicadas em uma só: `volta × (per × paraOrigem)`.
   - Aplicada a um ponto, ela age **da direita para a esquerda**: primeiro `paraOrigem`, depois `per`, depois `volta`.
   - O método `multiplicaMatrizes`, que já existia, usa **só os dois parâmetros**. Chamá-lo "a partir de" `projecao` é apenas a forma que a classe oferece.
6. O resultado vira a nova `projecao`, usada por todos os triângulos no próximo desenho.
7. A fórmula final é `xp = cx + d·(x − cx)/(z + d)`, e o mesmo vale para `y`.

**Objetivo:** a matriz do slide põe o centro de projeção sobre a origem, que na tela é o **canto superior
esquerdo**. Sem as translações, tudo convergiria para esse canto. Com elas, o ponto de fuga fica no
meio da janela. É a mesma técnica de "levar para a origem, transformar e voltar" usada para girar em
torno de um ponto.

---

## 19. `simulaMundo(...)`: movimento por tempo

[MainCanvas.java:614-641](src/MainCanvas.java#L614-L641)

**Antes (main):**

```java
float difS = diftime/1000.0f;
float vel = 50;

timer+=diftime;

if(UP || DOWN || LEFT || RIGHT) {
    Mat4x4 matrot = new Mat4x4();
    if (UP) {
        matrot.setTranslate(0, -1,0);
    }
    if (DOWN) {
        matrot.setTranslate(0, +1,0);
    }
    if (LEFT) {
        matrot.setTranslate(-1, 0,0);
    }
    if (RIGHT) {
        matrot.setTranslate(+1, 0,0);
    }

    Mat4x4 mr = modelview.multiplicaMatrizes(matrot,modelview);
    modelview = mr;
}
```

**Agora (ajustes):**

```java
float difS = diftime/1000.0f;
float vel = 200; // pixels por segundo

timer+=diftime;

if(UP || DOWN || LEFT || RIGHT) {
    // deslocamento proporcional ao tempo do quadro: a ~1000 FPS,
    // 1 pixel por quadro jogava a cena para fora da tela num instante
    float dx = 0;
    float dy = 0;
    if (UP) {
        dy -= vel*difS;
    }
    if (DOWN) {
        dy += vel*difS;
    }
    if (LEFT) {
        dx -= vel*difS;
    }
    if (RIGHT) {
        dx += vel*difS;
    }
    Mat4x4 matrot = new Mat4x4();
    matrot.setTranslate(dx, dy, 0);

    Mat4x4 mr = modelview.multiplicaMatrizes(matrot,modelview);
    modelview = mr;
}
```

**O que faz:** move a cena com `W`/`A`/`S`/`D` a uma velocidade fixa de **200 pixels por segundo**,
inclusive na diagonal.

**Como funciona:**
- `simulaMundo` é chamado **uma vez por quadro**. `diftime` é quanto tempo passou desde o quadro anterior,
  em milissegundos, e `difS` é o mesmo valor em segundos.
- **Antes**, cada quadro movia **1 pixel**. Com o programa a cerca de 1000 quadros por segundo, isso dava
  cerca de 1000 pixels por segundo, e a cena sumia da tela com um toque.
- **Agora**, o deslocamento é `velocidade × tempo` (`vel*difS`). Em computador rápido ou lento, a cena
  anda sempre 200 pixels por segundo. As variáveis `vel` e `difS` já existiam, mas não eram usadas.
- **Antes**, cada tecla chamava `setTranslate`, que **apaga** a matriz e a refaz. Com duas teclas
  apertadas, só a última valia. **Agora**, `dx` e `dy` **somam** as teclas, e só no fim a translação é
  montada uma vez. `W` + `A` movem na diagonal.
- A última parte (juntar a translação com a `modelview`) não mudou.

**Objetivo:** tornar `WASD` utilizável. Antes, parecia que "as teclas não funcionavam", porque a cena
sumia instantaneamente.

---

## 20. `paint(...)`: fundo do tamanho da janela

[MainCanvas.java:674](src/MainCanvas.java#L674)

**Antes (main):**

```java
g.fillRect(0, 0, 800, 600);
```

**Agora (ajustes):**

```java
g.fillRect(0, 0, getWidth(), getHeight());
```

**O que faz:** pinta o fundo branco do painel inteiro, qualquer que seja o tamanho.

**Como funciona:** `getWidth()` e `getHeight()` devolvem o tamanho **atual** do painel, em vez do valor
fixo 800×600.

**Objetivo:** com a janela maximizada, a área fora de 800×600 não era limpa, ficava cinza e guardava
restos de desenhos antigos.

---

## 21. `paint(...)`: legenda e ajuda na tela

[MainCanvas.java:707-711](src/MainCanvas.java#L707-L711)

```java
g.drawString("Projecao: "+nomeProjecao, 10, getHeight()-15);

g.setFont(fAjuda);
g.drawString("1 Paralela | 2 Cavalier | 3 Cabinet | 4 Perspectiva (R/F muda d)", 10, getHeight()-70);
g.drawString("WASD move | Q/E gira | Z/X escala | Espaco reinicia a cena", 10, getHeight()-52);
```

**O que faz:** escreve no rodapé da janela a projeção atual (ex.: `Projecao: Perspectiva d=350`) e,
acima dela, duas linhas com todas as teclas.

**Como funciona:**
- `drawString(texto, x, y)` escreve o texto na posição dada.
- `y = getHeight() − 15` coloca a legenda sempre **15 pixels acima do fundo**, em qualquer tamanho de janela.
- A legenda usa a fonte grande `f`, que já estava ativa. Depois, `setFont(fAjuda)` troca para a fonte
  pequena (item 8) para as duas linhas de ajuda.
- A troca de fonte não atrapalha o próximo quadro, porque o `paint` começa com `g.setFont(f)`.

**Objetivo:** deixar visível **qual projeção está ativa** e qual o `d`, e mostrar as teclas para quem usa
o programa pela primeira vez.

---

## 22. `run()`: redesenhar a janela inteira

[MainCanvas.java:765](src/MainCanvas.java#L765)

**Antes (main):**

```java
paintImmediately(0, 0, 640, 480);
```

**Agora (ajustes):**

```java
paintImmediately(0, 0, getWidth(), getHeight()); // janela inteira, mesmo maximizada
```

**O que faz:** redesenha, a cada quadro, o painel **inteiro**.

**Como funciona:** `run()` é o laço principal. Ele roda sem parar: simula (`simulaMundo`), desenha
(`paintImmediately`), mede o FPS e repete. `paintImmediately(x, y, largura, altura)` redesenha só o
retângulo informado. Antes era fixo em 640×480; agora é o tamanho real do painel.

**Objetivo:** com a janela maior que 640×480 (maximizada, por exemplo), tudo fora dessa área ficava
**congelado**: a legenda, os triângulos desenhados com o mouse e a cena deslocada.

---

# MainClass.java

## 23. Painel antes de mostrar a janela, e foco

[MainClass.java:10-14](src/MainClass.java#L10-L14)

**Antes (main):**

```java
JFrame f = new JFrame();
f.setSize(640, 480);
f.setVisible(true);
f.getContentPane().add(meuCanvas);
```

**Agora (ajustes):**

```java
JFrame f = new JFrame();
f.setSize(640, 480);
f.getContentPane().add(meuCanvas); // adiciona antes de mostrar a janela
f.setVisible(true);
meuCanvas.requestFocusInWindow(); // teclas vao direto para o painel
```

**O que faz:** coloca o painel na janela **antes** de ela aparecer e, logo depois de aparecer, dá o foco
do teclado para ele.

**Como funciona:**
- A ordem de `add` e `setVisible` foi trocada. A janela já é montada completa quando aparece.
- `requestFocusInWindow()` pede o foco para o painel. Ele precisa vir **depois** do `setVisible`, porque
  um componente só recebe foco se estiver visível na tela.

**Objetivo:** garantir que as teclas funcionem assim que o programa abre. Antes, a janela aparecia vazia e o
painel entrava depois, e dependendo de como o programa era aberto, o painel ficava sem foco. Montar
tudo antes de mostrar a janela é a prática recomendada no Swing.

---

# bin/.gitignore

## 24. Ignorar `MainCanvas$4.class`

[bin/.gitignore:4](bin/.gitignore#L4)

```
/MainCanvas$4.class
```

**O que faz:** diz ao Git para não versionar o arquivo compilado `MainCanvas$4.class`.

**Como funciona:** cada classe anônima (`new KeyListener() {...}`, `new ComponentAdapter() {...}` etc.)
vira um arquivo `.class` separado, numerado na ordem em que aparece no código.

| Arquivo | Na `main` | Na `ajustes` |
|---|---|---|
| `MainCanvas$1` | `KeyListener` | `KeyListener` |
| `MainCanvas$2` | `MouseListener` | `MouseListener` |
| `MainCanvas$3` | `MouseMotionListener` | `ComponentAdapter` (o novo listener, item 17) |
| `MainCanvas$4` | não existia | `MouseMotionListener` |

O novo listener foi escrito **antes** do `MouseMotionListener`, então ficou com o número 3 e empurrou o
`MouseMotionListener` para o 4. Com uma classe anônima a mais, passa a existir o arquivo
`MainCanvas$4.class`. O `bin/.gitignore` é gerado pelo Eclipse e só listava do `$1` ao `$3`.

**Objetivo:** manter o repositório só com o código-fonte. Arquivos compilados são gerados de novo a cada
compilação e não devem ir para o Git.
