import { useEffect, useMemo, useState } from 'react';
import { Alert, FlatList, KeyboardAvoidingView, Linking, Modal, Platform, Pressable, SafeAreaView, ScrollView, StatusBar, StyleSheet, Text, TextInput, View } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import * as AuthSession from 'expo-auth-session';
import { supabase } from './src/lib/supabase';

WebBrowser.maybeCompleteAuthSession();

const money = (v) => new Intl.NumberFormat('uz-UZ').format(Number(v || 0)) + " so'm";
const callbackUrl = AuthSession.makeRedirectUri({ scheme: 'qarzdaftari', path: 'auth/callback' });

export default function App() {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [debts, setDebts] = useState([]);
  const [tab, setTab] = useState('home');
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState({ name: '', amount: '', returned: '', direction: 'received' });
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!supabase) { setLoading(false); return; }
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setLoading(false);
    });
    const { data: listener } = supabase.auth.onAuthStateChange((_event, next) => setSession(next));
    return () => listener.subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (session) loadDebts();
    else setDebts([]);
  }, [session]);

  async function loadDebts() {
    const { data, error } = await supabase.from('debts').select('*').eq('user_id', session.user.id).order('created_at', { ascending: false });
    if (error) Alert.alert('Xatolik', 'Qarzlarni yuklab bo‘lmadi.');
    else setDebts(data || []);
  }

  async function googleLogin() {
    if (!supabase) return Alert.alert('Sozlama kerak', '.env faylini to‘ldiring.');
    const { data, error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: callbackUrl, skipBrowserRedirect: true }
    });
    if (error) return Alert.alert('Google Login', error.message);
    const result = await WebBrowser.openAuthSessionAsync(data.url, callbackUrl);
    if (result.type === 'success') {
      const url = result.url;
      const hash = url.split('#')[1] || '';
      const params = new URLSearchParams(hash);
      const access_token = params.get('access_token');
      const refresh_token = params.get('refresh_token');
      if (access_token && refresh_token) await supabase.auth.setSession({ access_token, refresh_token });
    }
  }

  async function addDebt() {
    if (!form.name.trim() || !Number(form.amount)) return Alert.alert('Tekshiring', 'Ism va miqdorni kiriting.');
    setBusy(true);
    const { data, error } = await supabase.from('debts').insert({
      user_id: session.user.id,
      name: form.name.trim(),
      amount: Number(form.amount),
      returned: Number(form.returned || 0),
      direction: form.direction
    }).select().single();
    setBusy(false);
    if (error) return Alert.alert('Xatolik', 'Qarz saqlanmadi.');
    setDebts(d => [data, ...d]);
    setForm({ name: '', amount: '', returned: '', direction: 'received' });
    setModal(false);
  }

  async function logout() {
    await supabase.auth.signOut();
  }

  const incoming = debts.filter(d => d.direction === 'received');
  const outgoing = debts.filter(d => d.direction === 'given');
  const totalIncoming = incoming.reduce((s,d) => s + Number(d.amount || 0), 0);
  const totalOutgoing = outgoing.reduce((s,d) => s + Number(d.amount || 0), 0);
  const totalReturned = debts.reduce((s,d) => s + Number(d.returned || 0), 0);
  const visible = tab === 'qarzdorman' ? outgoing : tab === 'qarzdorlar' ? incoming : debts;

  if (loading) return <Screen><Text style={styles.loading}>Yuklanmoqda...</Text></Screen>;
  if (!session) return <Login onLogin={googleLogin} configured={!!supabase} />;

  return (
    <Screen>
      <View style={styles.header}>
        <View><Text style={styles.eyebrow}>QARZ DAFTARI</Text><Text style={styles.title}>Boshqaruv paneli</Text></View>
        <Pressable onPress={logout} style={styles.iconButton}><Text style={styles.iconText}>↪</Text></Pressable>
      </View>

      {tab === 'home' ? (
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.hero}>
            <Text style={styles.heroLabel}>Umumiy qarzlar</Text>
            <Text style={styles.heroAmount}>{money(totalIncoming + totalOutgoing)}</Text>
            <Text style={styles.heroSub}>{session.user.user_metadata?.full_name || session.user.email}</Text>
          </View>
          <View style={styles.cards}>
            <Summary title="Olgan qarzlar" value={money(totalIncoming)} count={incoming.length} />
            <Summary title="Bergan qarzlar" value={money(totalOutgoing)} count={outgoing.length} />
            <Summary title="Qaytarganlar" value={money(totalReturned)} count={debts.length} />
          </View>
          <Pressable style={styles.primary} onPress={() => setModal(true)}><Text style={styles.primaryText}>+ Qarz qo‘shish</Text></Pressable>
          <Text style={styles.sectionTitle}>So‘nggi qarzlar</Text>
          {debts.slice(0, 5).map(d => <DebtRow key={d.id} debt={d} />)}
          {!debts.length && <Empty />}
        </ScrollView>
      ) : (
        <View style={styles.listWrap}>
          <View style={styles.pageHead}><Text style={styles.sectionTitle}>{tab === 'qarzdorman' ? 'Qarzdorman' : 'Qarzdorlar'}</Text><Text style={styles.muted}>{visible.length} ta yozuv</Text></View>
          <FlatList data={visible} keyExtractor={x => x.id} renderItem={({ item }) => <DebtRow debt={item} />} ListEmptyComponent={<Empty />} contentContainerStyle={styles.list} />
        </View>
      )}

      <View style={styles.nav}>
        <Nav label="Asosiy" active={tab === 'home'} onPress={() => setTab('home')} />
        <Nav label="Qarzdorman" active={tab === 'qarzdorman'} onPress={() => setTab('qarzdorman')} />
        <Nav label="Qarzdorlar" active={tab === 'qarzdorlar'} onPress={() => setTab('qarzdorlar')} />
      </View>

      <Modal visible={modal} transparent animationType="slide" onRequestClose={() => setModal(false)}>
        <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalBg}>
          <View style={styles.modal}>
            <Text style={styles.modalTitle}>Yangi qarz</Text>
            <TextInput placeholder="Ism" value={form.name} onChangeText={v => setForm({...form,name:v})} style={styles.input} />
            <TextInput placeholder="Miqdor (so‘m)" keyboardType="numeric" value={form.amount} onChangeText={v => setForm({...form,amount:v})} style={styles.input} />
            <TextInput placeholder="Qaytarilgan miqdor" keyboardType="numeric" value={form.returned} onChangeText={v => setForm({...form,returned:v})} style={styles.input} />
            <View style={styles.segment}>
              <Pressable onPress={() => setForm({...form,direction:'received'})} style={[styles.segmentBtn, form.direction === 'received' && styles.segmentActive]}><Text>Men qarz oldim</Text></Pressable>
              <Pressable onPress={() => setForm({...form,direction:'given'})} style={[styles.segmentBtn, form.direction === 'given' && styles.segmentActive]}><Text>Men qarz berdim</Text></Pressable>
            </View>
            <Pressable disabled={busy} onPress={addDebt} style={styles.primary}><Text style={styles.primaryText}>{busy ? 'Saqlanmoqda...' : 'Saqlash'}</Text></Pressable>
            <Pressable onPress={() => setModal(false)} style={styles.cancel}><Text>Bekor qilish</Text></Pressable>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </Screen>
  );
}

function Screen({children}) { return <SafeAreaView style={styles.safe}><StatusBar barStyle="dark-content" /><View style={styles.screen}>{children}</View></SafeAreaView>; }
function Summary({title,value,count}) { return <View style={styles.summary}><Text style={styles.summaryTitle}>{title}</Text><Text style={styles.summaryValue}>{value}</Text><Text style={styles.muted}>{count} ta</Text></View>; }
function DebtRow({debt}) { const remaining=Math.max(Number(debt.amount)-Number(debt.returned),0); return <View style={styles.row}><View style={styles.avatar}><Text style={styles.avatarText}>{debt.name?.[0]?.toUpperCase() || '?'}</Text></View><View style={styles.rowMain}><Text style={styles.rowName}>{debt.name}</Text><Text style={styles.muted}>{debt.direction === 'received' ? 'Men qarzdorman' : 'Menga qarzdor'}</Text></View><View><Text style={styles.rowAmount}>{money(remaining)}</Text><Text style={styles.muted}>qoldiq</Text></View></View>; }
function Empty() { return <View style={styles.empty}><Text style={styles.emptyTitle}>Hozircha qarzlar yo‘q</Text><Text style={styles.muted}>“Qarz qo‘shish” orqali birinchi yozuvni kiriting.</Text></View>; }
function Nav({label,active,onPress}) { return <Pressable onPress={onPress} style={styles.navItem}><Text style={[styles.navLabel,active&&styles.navActive]}>{label}</Text></Pressable>; }

function Login({onLogin,configured}) {
  return <Screen><View style={styles.login}><View style={styles.logo}><Text style={styles.logoText}>Q</Text></View><Text style={styles.loginTitle}>Qarz Daftari</Text><Text style={styles.loginSub}>Qarzlaringizni tartibli va xavfsiz boshqaring.</Text><Pressable onPress={onLogin} style={styles.google}><Text style={styles.googleG}>G</Text><Text style={styles.googleText}>Qarz Daftariga kirish</Text></Pressable>{!configured&&<Text style={styles.error}>Supabase .env sozlamalari topilmadi.</Text>}<Text style={styles.loginNote}>Google orqali xavfsiz kirish</Text></View></Screen>;
}

const styles = StyleSheet.create({
  safe:{flex:1,backgroundColor:'#f8fafc'},screen:{flex:1,paddingHorizontal:18},loading:{marginTop:80,textAlign:'center',fontSize:18,color:'#475569'},
  header:{paddingTop:18,paddingBottom:14,flexDirection:'row',justifyContent:'space-between',alignItems:'center'},eyebrow:{fontSize:11,letterSpacing:1.5,fontWeight:'800',color:'#6366f1'},title:{fontSize:23,fontWeight:'800',color:'#0f172a',marginTop:3},iconButton:{width:42,height:42,borderRadius:14,backgroundColor:'#eef2ff',alignItems:'center',justifyContent:'center'},iconText:{fontSize:23,color:'#4f46e5'},
  content:{paddingBottom:100},hero:{backgroundColor:'#4f46e5',borderRadius:24,padding:22,marginBottom:14},heroLabel:{color:'#c7d2fe',fontWeight:'700'},heroAmount:{color:'#fff',fontSize:30,fontWeight:'900',marginVertical:8},heroSub:{color:'#e0e7ff'},cards:{gap:10,marginBottom:14},summary:{backgroundColor:'#fff',borderRadius:18,padding:17,borderWidth:1,borderColor:'#e2e8f0'},summaryTitle:{fontWeight:'700',color:'#475569'},summaryValue:{fontSize:20,fontWeight:'900',color:'#0f172a',marginVertical:5},muted:{color:'#64748b',fontSize:12},
  primary:{backgroundColor:'#4f46e5',borderRadius:15,minHeight:50,alignItems:'center',justifyContent:'center',marginVertical:8},primaryText:{color:'#fff',fontWeight:'800',fontSize:15},sectionTitle:{fontSize:19,fontWeight:'850',color:'#0f172a',marginTop:12,marginBottom:10},
  row:{backgroundColor:'#fff',borderRadius:16,padding:14,marginBottom:9,flexDirection:'row',alignItems:'center',borderWidth:1,borderColor:'#e2e8f0'},avatar:{width:42,height:42,borderRadius:14,backgroundColor:'#ede9fe',alignItems:'center',justifyContent:'center',marginRight:11},avatarText:{fontWeight:'900',color:'#6d28d9'},rowMain:{flex:1},rowName:{fontWeight:'800',fontSize:15,color:'#0f172a',marginBottom:3},rowAmount:{fontWeight:'900',color:'#0f172a',textAlign:'right'},
  nav:{position:'absolute',bottom:0,left:0,right:0,height:66,backgroundColor:'#fff',borderTopWidth:1,borderTopColor:'#e2e8f0',flexDirection:'row'},navItem:{flex:1,alignItems:'center',justifyContent:'center'},navLabel:{fontSize:12,color:'#64748b',fontWeight:'700'},navActive:{color:'#4f46e5'},listWrap:{flex:1},list:{paddingBottom:85},pageHead:{flexDirection:'row',alignItems:'baseline',justifyContent:'space-between'},
  modalBg:{flex:1,justifyContent:'flex-end',backgroundColor:'rgba(15,23,42,.45)'},modal:{backgroundColor:'#fff',borderTopLeftRadius:26,borderTopRightRadius:26,padding:22,paddingBottom:30},modalTitle:{fontSize:23,fontWeight:'900',marginBottom:16,color:'#0f172a'},input:{height:50,borderWidth:1,borderColor:'#cbd5e1',borderRadius:13,paddingHorizontal:14,marginBottom:10,fontSize:15},segment:{flexDirection:'row',gap:8,marginBottom:8},segmentBtn:{flex:1,padding:13,borderRadius:12,backgroundColor:'#f1f5f9',alignItems:'center'},segmentActive:{backgroundColor:'#ddd6fe'},cancel:{alignItems:'center',padding:12},
  login:{flex:1,justifyContent:'center',alignItems:'center',paddingHorizontal:16},logo:{width:78,height:78,borderRadius:24,backgroundColor:'#4f46e5',alignItems:'center',justifyContent:'center',marginBottom:18},logoText:{fontSize:42,fontWeight:'900',color:'#fff'},loginTitle:{fontSize:31,fontWeight:'900',color:'#0f172a'},loginSub:{textAlign:'center',color:'#64748b',fontSize:15,lineHeight:22,marginTop:9,marginBottom:28},google:{height:54,width:'100%',maxWidth:380,borderRadius:15,backgroundColor:'#fff',borderWidth:1,borderColor:'#cbd5e1',flexDirection:'row',alignItems:'center',justifyContent:'center',gap:12},googleG:{fontSize:20,fontWeight:'900'},googleText:{fontSize:15,fontWeight:'800',color:'#0f172a'},loginNote:{color:'#94a3b8',fontSize:12,marginTop:14},error:{color:'#dc2626',marginTop:15,textAlign:'center'}
});
